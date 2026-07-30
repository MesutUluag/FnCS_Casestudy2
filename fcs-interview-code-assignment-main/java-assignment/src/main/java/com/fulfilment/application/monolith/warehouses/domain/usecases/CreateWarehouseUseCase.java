package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {

    // Business Unit Code must be unique
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new IllegalArgumentException(
          "A warehouse with business unit code '"
              + warehouse.businessUnitCode
              + "' already exists.");
    }

    // Location must exist
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Location '" + warehouse.location + "' does not exist.");
    }

    // Count active (non-archived) warehouses at this location
    List<Warehouse> warehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> warehouse.location.equals(w.location) && w.archivedAt == null)
            .toList();

    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Location '"
              + warehouse.location
              + "' has reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ").");
    }

    // Warehouse capacity must not exceed location max capacity
    if (warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Warehouse capacity ("
              + warehouse.capacity
              + ") exceeds the maximum capacity for location '"
              + warehouse.location
              + "' ("
              + location.maxCapacity
              + ").");
    }

    // Stock must not exceed warehouse capacity
    if (warehouse.stock != null && warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException(
          "Stock (" + warehouse.stock + ") cannot exceed warehouse capacity (" + warehouse.capacity + ").");
    }

    warehouse.createdAt = LocalDateTime.now();

    warehouseStore.create(warehouse);
  }
}
