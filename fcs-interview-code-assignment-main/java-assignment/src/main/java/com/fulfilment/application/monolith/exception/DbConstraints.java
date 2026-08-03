package com.fulfilment.application.monolith.exception;

/**
 * Central registry of every named database unique-constraint used across the application.
 *
 * <p>Each constant is used in two places:
 * <ol>
 *   <li>The {@code name} attribute of the {@code @UniqueConstraint} annotation on the entity.
 *   <li>The {@link GlobalExceptionMapper} lookup map that converts a violated constraint name
 *       into a human-readable 409 response.
 * </ol>
 *
 * <p>This single source of truth ensures that renaming a constraint is a one-line change
 * and that the mapper can match by exact code rather than fragile string scanning.
 */
public final class DbConstraints {

  private DbConstraints() {}

  // ── Store ─────────────────────────────────────────────────────────────────
  /** Unique store name: {@code store.name} */
  public static final String STORE_NAME = "uq_store_name";

  // ── Product ───────────────────────────────────────────────────────────────
  /** Unique product name: {@code product.name} */
  public static final String PRODUCT_NAME = "uq_product_name";

  // ── Warehouse ─────────────────────────────────────────────────────────────
  /** Unique warehouse business unit code: {@code warehouse.businessUnitCode} */
  public static final String WAREHOUSE_BUSINESS_UNIT_CODE = "uq_warehouse_businessunitcode";

  // ── Fulfilment association ────────────────────────────────────────────────
  /** Unique (warehouseId, storeId, productId) triple in {@code fulfilment_association} */
  public static final String FULFILMENT_ASSOCIATION_TRIPLE = "uq_fulfilment_warehouse_store_product";
}
