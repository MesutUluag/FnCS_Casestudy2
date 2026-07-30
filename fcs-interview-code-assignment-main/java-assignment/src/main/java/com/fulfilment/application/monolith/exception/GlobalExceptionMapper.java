package com.fulfilment.application.monolith.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

/**
 * Global exception mapper that covers every exception not already handled by a more specific
 * mapper. It walks the full cause chain to identify well-known error conditions and returns
 * user-friendly responses with appropriate HTTP status codes.
 *
 * <p>Handles:
 * <ul>
 *   <li>SQL state 23505 (unique_violation) → 409 Conflict
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

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {

    // 1. Pass-through for intentionally thrown HTTP responses (404, 422, etc.)
    if (exception instanceof WebApplicationException wae) {
      return wae.getResponse();
    }

    // 2. Walk the full cause chain looking for a constraint violation
    if (isUniqueConstraintViolation(exception)) {
      LOGGER.warnf("Unique constraint violation: %s", rootMessage(exception));
      return jsonResponse(409, "A record with the same unique value already exists. "
          + "Please check for duplicate names or codes.");
    }

    // 3. Everything else: log the full detail server-side, return a generic 400 to the client
    LOGGER.errorf(exception, "Unhandled exception: %s", exception.getClass().getName());
    return jsonResponse(400, "The request could not be completed. "
        + "Please check your input and try again.");
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  /** Returns true if any exception in the cause chain is a unique-constraint violation. */
  private boolean isUniqueConstraintViolation(Throwable t) {
    while (t != null) {
      if (t instanceof ConstraintViolationException cve
          && SQL_STATE_UNIQUE_VIOLATION.equals(cve.getSQLState())) {
        return true;
      }
      // Also catch raw JDBC PSQLException that may appear without Hibernate wrapper
      if (t instanceof java.sql.SQLException sqle
          && SQL_STATE_UNIQUE_VIOLATION.equals(sqle.getSQLState())) {
        return true;
      }
      t = t.getCause();
    }
    return false;
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
