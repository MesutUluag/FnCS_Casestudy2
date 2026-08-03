package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseConstraintException;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseConstraintException.Constraint;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Enforces the five warehouse creation rules before persisting a new warehouse:
 *
 * <ol>
 *   <li>BUC must be unique — no two active warehouses share the same business unit code.
 *   <li>Location must exist — the referenced location must be resolvable.
 *   <li>Location warehouse limit — the location must still have capacity for another warehouse.
 *   <li>Capacity ≤ location max capacity — the warehouse capacity must not exceed the location's
 *       maximum.
 *   <li>Stock ≤ capacity — initial stock must not exceed the warehouse capacity.
 * </ol>
 *
 * <p>Violations are reported as {@link WarehouseConstraintException} so the REST layer can return
 * a structured, human-readable 422 response to the frontend.
 */
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

    // Rule 1: BUC must be unique
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseConstraintException(
          Constraint.DUPLICATE_BUSINESS_UNIT_CODE,
          "A warehouse with business unit code '"
              + warehouse.businessUnitCode
              + "' already exists. Each warehouse must have a unique business unit code.");
    }

    // Rule 2: Location must exist
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseConstraintException(
          Constraint.LOCATION_NOT_FOUND,
          "The location '" + warehouse.location + "' does not exist. "
              + "Please provide a valid location identifier.");
    }

    // Rule 3: Location cannot exceed its warehouse limit
    List<Warehouse> warehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> warehouse.location.equals(w.location) && w.archivedAt == null)
            .toList();

    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseConstraintException(
          Constraint.LOCATION_WAREHOUSE_LIMIT_EXCEEDED,
          "The location '"
              + warehouse.location
              + "' has reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + "). Please choose a different location or archive an existing warehouse there first.");
    }

    // Rule 4: Capacity must not exceed location max capacity
    if (warehouse.capacity > location.maxCapacity) {
      throw new WarehouseConstraintException(
          Constraint.CAPACITY_EXCEEDS_LOCATION_MAX,
          "The requested capacity ("
              + warehouse.capacity
              + ") exceeds the maximum allowed capacity for location '"
              + warehouse.location
              + "' ("
              + location.maxCapacity
              + "). Please reduce the capacity or choose a different location.");
    }

    // Rule 5: Stock must not exceed warehouse capacity
    if (warehouse.stock != null && warehouse.stock > warehouse.capacity) {
      throw new WarehouseConstraintException(
          Constraint.STOCK_EXCEEDS_CAPACITY,
          "The initial stock ("
              + warehouse.stock
              + ") cannot exceed the warehouse capacity ("
              + warehouse.capacity
              + "). Please reduce the stock or increase the capacity.");
    }

    warehouse.createdAt = LocalDateTime.now();

    warehouseStore.create(warehouse);
  }
}
