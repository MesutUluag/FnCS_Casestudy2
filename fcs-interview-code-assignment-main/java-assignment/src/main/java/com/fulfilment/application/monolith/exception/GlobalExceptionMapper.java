package com.fulfilment.application.monolith.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

/**
 * Global exception mapper that covers every exception not already handled by a more specific
 * mapper. It walks the full cause chain to identify well-known error conditions and returns
 * user-friendly responses with appropriate HTTP status codes.
 *
 * <p>Handles:
 * <ul>
 *   <li>SQL state 23505 (unique_violation) → 409 Conflict, with a constraint-specific message
 *       resolved from {@link #CONSTRAINT_MESSAGES} using the exact DB constraint name.
 *   <li>{@link WebApplicationException} → its own status code (pass-through)
 *   <li>Everything else → 400 Bad Request with a generic message
 * </ul>
 *
 * <p>Quarkus Arc wraps transactional commit failures in {@code ArcUndeclaredThrowableException}
 * which in turn wraps {@code RollbackException} → {@code ConstraintViolationException}. Catching
 * {@code Exception} at this level ensures those wrappings are always unwrapped correctly.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class);
  private static final String SQL_STATE_UNIQUE_VIOLATION = "23505";

  /**
   * Maps each named unique constraint (defined in {@link DbConstraints}) to the human-readable
   * message returned to the frontend on a 409 Conflict response.
   *
   * <p>Add a new entry here whenever a new unique constraint is added to any entity.
   */
  private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
      DbConstraints.STORE_NAME,
          "A store with that name already exists. Please choose a different store name.",
      DbConstraints.PRODUCT_NAME,
          "A product with that name already exists. Please choose a different product name.",
      DbConstraints.WAREHOUSE_BUSINESS_UNIT_CODE,
          "A warehouse with that business unit code already exists. "
              + "Each warehouse must have a unique business unit code.",
      DbConstraints.FULFILMENT_ASSOCIATION_TRIPLE,
          "This warehouse-store-product combination already exists. "
              + "Each fulfilment association must be unique."
  );

  private static final String FALLBACK_CONFLICT_MESSAGE =
      "A record with the same unique value already exists. "
          + "Please check for duplicate names or codes.";

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {

    // 1. Pass-through for intentionally thrown HTTP responses (404, 422, etc.)
    if (exception instanceof WebApplicationException wae) {
      return wae.getResponse();
    }

    // 2. Walk the full cause chain looking for a unique-constraint violation
    String violatedConstraint = extractViolatedConstraintName(exception);
    if (violatedConstraint != null) {
      String humanMessage = CONSTRAINT_MESSAGES.getOrDefault(
          violatedConstraint, FALLBACK_CONFLICT_MESSAGE);
      LOGGER.warnf("Unique constraint violation [%s]: %s", violatedConstraint, rootMessage(exception));
      return jsonResponse(409, humanMessage);
    }

    // 3. Everything else: log the full detail server-side, return a generic 400 to the client
    LOGGER.errorf(exception, "Unhandled exception: %s", exception.getClass().getName());
    return jsonResponse(400, "The request could not be completed. "
        + "Please check your input and try again.");
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  /**
   * Walks the cause chain and returns the constraint name if a 23505 unique-violation is found,
   * or {@code null} if no such violation exists in the chain.
   *
   * <p>Hibernate wraps the JDBC exception as {@link ConstraintViolationException} and exposes
   * the constraint name directly. For raw JDBC {@link java.sql.SQLException} (e.g. from drivers
   * that bypass Hibernate), the constraint name is parsed from the SQL message as a fallback.
   */
  private String extractViolatedConstraintName(Throwable t) {
    while (t != null) {
      if (t instanceof ConstraintViolationException cve
          && SQL_STATE_UNIQUE_VIOLATION.equals(cve.getSQLState())) {
        // Hibernate exposes the constraint name directly — use it when available
        String name = cve.getConstraintName();
        return (name != null && !name.isBlank()) ? name.toLowerCase() : "";
      }
      if (t instanceof java.sql.SQLException sqle
          && SQL_STATE_UNIQUE_VIOLATION.equals(sqle.getSQLState())) {
        // Raw JDBC: parse the constraint name from the message (PostgreSQL format:
        // "... unique constraint \"<name>\"")
        return parseConstraintNameFromMessage(sqle.getMessage());
      }
      t = t.getCause();
    }
    return null;
  }

  /**
   * Parses a constraint name from a PostgreSQL JDBC error message.
   * PostgreSQL formats the message as: {@code unique constraint "constraint_name"}
   */
  private String parseConstraintNameFromMessage(String message) {
    if (message == null) return "";
    // PostgreSQL format: ... unique constraint "constraint_name"
    int first = message.indexOf('"');
    int second = first >= 0 ? message.indexOf('"', first + 1) : -1;
    if (first >= 0 && second > first) {
      return message.substring(first + 1, second).toLowerCase();
    }
    return message.toLowerCase();
  }

  /** Returns the message of the deepest non-null cause. */
  private String rootMessage(Throwable t) {
    String msg = t.getMessage();
    while (t.getCause() != null) {
      t = t.getCause();
      if (t.getMessage() != null) msg = t.getMessage();
    }
    return msg;
  }

  private Response jsonResponse(int status, String message) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("code", status);
    body.put("error", message);
    return Response.status(status).entity(body).type("application/json").build();
  }
}
