package com.fulfilment.application.monolith.warehouses.domain.models;

/**
 * Thrown when a warehouse create or replace request violates one of the business rules:
 *
 * <ul>
 *   <li>{@link Constraint#DUPLICATE_BUSINESS_UNIT_CODE} — the BUC already exists.
 *   <li>{@link Constraint#LOCATION_NOT_FOUND} — the given location does not exist.
 *   <li>{@link Constraint#LOCATION_WAREHOUSE_LIMIT_EXCEEDED} — the location has no room for
 *       another warehouse.
 *   <li>{@link Constraint#CAPACITY_EXCEEDS_LOCATION_MAX} — the requested capacity exceeds the
 *       location's maximum allowed capacity.
 *   <li>{@link Constraint#STOCK_EXCEEDS_CAPACITY} — the initial stock exceeds the warehouse
 *       capacity.
 *   <li>{@link Constraint#REPLACEMENT_CAPACITY_BELOW_STOCK} — the replacement warehouse capacity
 *       is lower than the current stock of the warehouse being replaced.
 *   <li>{@link Constraint#REPLACEMENT_STOCK_MISMATCH} — the stock of the replacement warehouse
 *       does not match the stock of the warehouse being replaced.
 * </ul>
 *
 * <p>Callers (e.g. {@link com.fulfilment.application.monolith.warehouses.adapters.restapi.WarehouseResourceImpl})
 * convert this exception into an HTTP 422 response with a structured JSON body that is safe and
 * meaningful for frontend consumers.
 */
public class WarehouseConstraintException extends RuntimeException {

  /** Identifies which business rule was violated. */
  public enum Constraint {
    DUPLICATE_BUSINESS_UNIT_CODE,
    LOCATION_NOT_FOUND,
    LOCATION_WAREHOUSE_LIMIT_EXCEEDED,
    CAPACITY_EXCEEDS_LOCATION_MAX,
    STOCK_EXCEEDS_CAPACITY,
    REPLACEMENT_CAPACITY_BELOW_STOCK,
    REPLACEMENT_STOCK_MISMATCH
  }

  private final Constraint constraint;

  public WarehouseConstraintException(Constraint constraint, String humanReadableMessage) {
    super(humanReadableMessage);
    this.constraint = constraint;
  }

  /** The business rule that was violated. */
  public Constraint getConstraint() {
    return constraint;
  }
}
