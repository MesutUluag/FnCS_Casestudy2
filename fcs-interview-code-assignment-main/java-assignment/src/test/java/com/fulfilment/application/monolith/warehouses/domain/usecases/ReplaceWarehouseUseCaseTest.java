package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplaceWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;

  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ReplaceWarehouseUseCase(warehouseStore);
  }

  private Warehouse existingWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "MWH.001";
    w.location = "ZWOLLE-001";
    w.capacity = 100;
    w.stock = 10;
    w.archivedAt = null;
    return w;
  }

  private Warehouse newWarehouse() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "MWH.001";
    w.location = "ZWOLLE-002";
    w.capacity = 50;
    w.stock = 10; // must match existing stock
    return w;
  }

  @Test
  void replace_happyPath_shouldArchiveOldAndCreateNew() {
    Warehouse existing = existingWarehouse();
    Warehouse replacement = newWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    useCase.replace(replacement);

    assertNotNull(existing.archivedAt, "existing warehouse must be archived");
    assertNotNull(replacement.createdAt, "new warehouse must have createdAt set");
    verify(warehouseStore).update(existing);
    verify(warehouseStore).create(replacement);
  }

  @Test
  void replace_noExistingWarehouse_shouldThrow() {
    Warehouse replacement = newWarehouse();
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(null);

    assertThrows(NoSuchElementException.class, () -> useCase.replace(replacement));
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void replace_newCapacityLessThanExistingStock_shouldThrow() {
    Warehouse existing = existingWarehouse();
    existing.stock = 30;
    Warehouse replacement = newWarehouse();
    replacement.capacity = 20; // less than existing stock of 30
    replacement.stock = 30;
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void replace_stockMismatch_shouldThrow() {
    Warehouse existing = existingWarehouse();
    Warehouse replacement = newWarehouse();
    replacement.stock = 99; // does not match existing.stock = 10
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    verify(warehouseStore, never()).create(any());
  }
}
