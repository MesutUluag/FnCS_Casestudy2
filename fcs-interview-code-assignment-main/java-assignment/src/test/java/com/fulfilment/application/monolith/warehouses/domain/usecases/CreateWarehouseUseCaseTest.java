package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  private Warehouse validWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "MWH.NEW";
    w.location = "AMSTERDAM-001";
    w.capacity = 50;
    w.stock = 10;
    return w;
  }

  @Test
  void create_happyPath_shouldSetCreatedAtAndPersist() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(List.of());

    useCase.create(warehouse);

    assertNotNull(warehouse.createdAt);
    verify(warehouseStore).create(warehouse);
  }

  @Test
  void create_duplicateBusinessUnitCode_shouldThrow() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(new Warehouse());

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_unknownLocation_shouldThrow() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_locationAtMaxWarehouses_shouldThrow() {
    Warehouse warehouse = validWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 1, 100)); // max 1

    Warehouse existing = new Warehouse();
    existing.location = "AMSTERDAM-001";
    existing.archivedAt = null;
    when(warehouseStore.getAll()).thenReturn(List.of(existing)); // already 1

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_capacityExceedsLocationMax_shouldThrow() {
    Warehouse warehouse = validWarehouse();
    warehouse.capacity = 200; // exceeds location maxCapacity of 100
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(List.of());

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_stockExceedsCapacity_shouldThrow() {
    Warehouse warehouse = validWarehouse();
    warehouse.capacity = 10;
    warehouse.stock = 20; // stock > capacity
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(List.of());

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }
}
