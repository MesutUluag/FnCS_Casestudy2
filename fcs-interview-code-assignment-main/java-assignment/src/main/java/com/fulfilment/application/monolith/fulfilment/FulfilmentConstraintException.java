package com.fulfilment.application.monolith.fulfilment;

/**
 * Thrown when a fulfilment association request violates one of the four business rules:
 *
 * <ul>
 *   <li>{@link Constraint#DUPLICATE_ASSOCIATION} — the exact (warehouse, store, product) triple
 *       already exists.
 *   <li>{@link Constraint#MAX_WAREHOUSES_PER_PRODUCT_PER_STORE} — a product is already supplied
 *       to the target store by the maximum number of warehouses (2).
 *   <li>{@link Constraint#MAX_WAREHOUSES_PER_STORE} — the target store is already served by the
 *       maximum number of distinct warehouses (3).
 *   <li>{@link Constraint#MAX_PRODUCTS_PER_WAREHOUSE} — the target warehouse already holds the
 *       maximum number of distinct product types (5).
 * </ul>
 *
 * <p>Callers (e.g. {@link FulfilmentResource}) convert this exception into an HTTP 422 response
 * with a structured JSON body that is safe and meaningful for frontend consumers.
 */
public class FulfilmentConstraintException extends RuntimeException {

  /** Identifies which business rule was violated. */
  public enum Constraint {
    DUPLICATE_ASSOCIATION,
    MAX_WAREHOUSES_PER_PRODUCT_PER_STORE,
    MAX_WAREHOUSES_PER_STORE,
    MAX_PRODUCTS_PER_WAREHOUSE
  }

  private final Constraint constraint;

  public FulfilmentConstraintException(Constraint constraint, String humanReadableMessage) {
    super(humanReadableMessage);
    this.constraint = constraint;
  }

  /** The business rule that was violated. */
  public Constraint getConstraint() {
    return constraint;
  }
}
