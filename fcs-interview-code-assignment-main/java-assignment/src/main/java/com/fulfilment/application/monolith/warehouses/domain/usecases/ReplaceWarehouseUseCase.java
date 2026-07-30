package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

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
      throw new IllegalArgumentException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the current stock ("
              + existing.stock
              + ") of the warehouse being replaced.");
    }

    // New warehouse stock must match existing warehouse stock
    if (!existing.stock.equals(newWarehouse.stock)) {
      throw new IllegalArgumentException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock of the warehouse being replaced ("
              + existing.stock
              + ").");
    }

    // Archive the existing warehouse
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    // Create the new warehouse
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
