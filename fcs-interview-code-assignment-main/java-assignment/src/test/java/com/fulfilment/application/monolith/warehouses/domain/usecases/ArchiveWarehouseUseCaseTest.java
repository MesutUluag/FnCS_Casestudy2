package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArchiveWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;

  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void archive_shouldSetArchivedAtAndCallUpdate() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 100;
    warehouse.stock = 10;

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt, "archivedAt must be set after archive()");
    verify(warehouseStore).update(warehouse);
    verify(warehouseStore, never()).create(any());
  }
}
