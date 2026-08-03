package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseConstraintException;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseConstraintException.Constraint;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Replaces an active warehouse with a new one, enforcing:
 *
 * <ul>
 *   <li>The warehouse to replace must exist and be active (404 if not found).
 *   <li>{@link Constraint#REPLACEMENT_CAPACITY_BELOW_STOCK} — new capacity must cover current
 *       stock.
 *   <li>{@link Constraint#REPLACEMENT_STOCK_MISMATCH} — new warehouse stock must equal current
 *       stock.
 * </ul>
 *
 * <p>Violations are reported as {@link WarehouseConstraintException} so the REST layer can return
 * a structured, human-readable 422 response to the frontend.
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void replace(Warehouse newWarehouse) {

    // Find the existing active warehouse with the same business unit code
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null || existing.archivedAt != null) {
      throw new NoSuchElementException(
          "No active warehouse found with business unit code '"
              + newWarehouse.businessUnitCode
              + "'.");
    }

    // New warehouse capacity must accommodate existing warehouse stock
    if (newWarehouse.capacity < existing.stock) {
      throw new WarehouseConstraintException(
          Constraint.REPLACEMENT_CAPACITY_BELOW_STOCK,
          "The new warehouse capacity ("
              + newWarehouse.capacity
              + ") is too low to accommodate the current stock ("
              + existing.stock
              + ") of the warehouse being replaced. "
              + "Please increase the capacity to at least " + existing.stock + ".");
    }

    // New warehouse stock must match existing warehouse stock
    if (!existing.stock.equals(newWarehouse.stock)) {
      throw new WarehouseConstraintException(
          Constraint.REPLACEMENT_STOCK_MISMATCH,
          "The replacement warehouse stock ("
              + newWarehouse.stock
              + ") must match the current stock of the warehouse being replaced ("
              + existing.stock
              + "). Stock cannot change during a warehouse replacement.");
    }

    // Archive the existing warehouse
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    // Create the new warehouse
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
