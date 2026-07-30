package com.fulfilment.application.monolith.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

/**
 * Global mapper for JPA/Hibernate exceptions. Converts constraint violations (duplicate key,
 * unique index, etc.) to 409 Conflict with a user-friendly message instead of a raw 500.
 *
 * <p>This covers all resources (Product, Store, Warehouse, Fulfilment) in one place.
 */
@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

  private static final Logger LOGGER = Logger.getLogger(PersistenceExceptionMapper.class);

  // PostgreSQL / JDBC SQL state for unique_violation
  private static final String SQL_STATE_UNIQUE_VIOLATION = "23505";

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(PersistenceException exception) {

    // Walk the cause chain looking for a ConstraintViolationException with SQL state 23505
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException cve) {
        String sqlState = cve.getSQLState();
        if (SQL_STATE_UNIQUE_VIOLATION.equals(sqlState)) {
          LOGGER.warnf("Unique constraint violation: %s", cve.getMessage());
          return conflictResponse("A record with the same unique value already exists. "
              + "Please check for duplicate names or codes.");
        }
      }
      cause = cause.getCause();
    }

    // Any other persistence problem → 500 with a generic message (no internal detail exposed)
    LOGGER.error("Unexpected persistence error", exception);
    return errorResponse(500, "An unexpected database error occurred. Please try again.");
  }

  private Response conflictResponse(String message) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("code", 409);
    body.put("error", message);
    return Response.status(409).entity(body).build();
  }

  private Response errorResponse(int code, String message) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("code", code);
    body.put("error", message);
    return Response.status(code).entity(body).build();
  }
}
