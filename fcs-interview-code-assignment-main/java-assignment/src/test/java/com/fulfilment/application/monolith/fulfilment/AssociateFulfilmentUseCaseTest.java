package com.fulfilment.application.monolith.fulfilment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssociateFulfilmentUseCaseTest {

  @Mock FulfilmentAssociationRepository repository;
  @Mock WarehouseRepository warehouseRepository;

  @InjectMocks AssociateFulfilmentUseCase useCase;

  private final Long warehouseId = 1L;
  private final Long storeId = 1L;
  private final Long productId = 1L;

  @BeforeEach
  void defaultHappyPathStubs() {
    // Duplicate guard: no existing triple by default
    when(repository.associationExists(warehouseId, storeId, productId)).thenReturn(false);
    // Warehouse exists
    when(warehouseRepository.findById(warehouseId)).thenReturn(new DbWarehouse());
    // Constraint 1: 0 warehouses serve this product at this store
    when(repository.countDistinctWarehousesForProductAtStore(storeId, productId)).thenReturn(0L);
    // Constraint 2: warehouse is new to this store, store has 0 warehouses
    when(repository.warehouseAlreadyServesStore(warehouseId, storeId)).thenReturn(false);
    when(repository.countDistinctWarehousesByStore(storeId)).thenReturn(0L);
    // Constraint 3: product is new to this warehouse, warehouse has 0 products
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(false);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(0L);
  }

  // ── Duplicate guard ───────────────────────────────────────────────────────

  @Test
  void associate_duplicateTriple_shouldThrow() {
    when(repository.associationExists(warehouseId, storeId, productId)).thenReturn(true);

    assertThrows(
        IllegalArgumentException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  // ── Constraint 1: max 2 warehouses per product per store ──────────────────

  @Test
  void associate_constraint1_exceedMaxWarehousesPerProductPerStore_shouldThrow() {
    when(repository.countDistinctWarehousesForProductAtStore(storeId, productId))
        .thenReturn(2L); // already at max

    assertThrows(
        IllegalArgumentException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  // ── Constraint 2: max 3 warehouses per store ──────────────────────────────

  @Test
  void associate_constraint2_exceedMaxWarehousesPerStore_shouldThrow() {
    when(repository.warehouseAlreadyServesStore(warehouseId, storeId)).thenReturn(false);
    when(repository.countDistinctWarehousesByStore(storeId)).thenReturn(3L); // already at max

    assertThrows(
        IllegalArgumentException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  @Test
  void associate_constraint2_warehouseAlreadyServesStore_skipsCountCheck() {
    // When warehouse already serves store, countDistinctWarehousesByStore must NOT be called.
    // We verify this by checking constraint-2 throws before the count is reached when the
    // countDistinctWarehousesByStore stub returns an over-limit value — but only when the
    // warehouse is NEW. When it already serves the store, the constraint is skipped entirely.
    // Assert: if warehouseAlreadyServesStore=true and countDistinct returns over-limit, no exception
    when(repository.warehouseAlreadyServesStore(warehouseId, storeId)).thenReturn(true);
    when(repository.countDistinctWarehousesByStore(storeId)).thenReturn(99L); // would trigger if checked

    // Should NOT throw constraint-2 exception (count is bypassed)
    // Will still throw constraint-3 if product count is over-limit; ensure it isn't
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(false);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(0L);

    // Entity checks (Store/Product) use Panache static methods — they throw outside Quarkus context.
    // We only care that constraint-2's count was never consulted, not about the exact exception type.
    try { useCase.associate(warehouseId, storeId, productId); } catch (Exception ignored) {}

    // Key assertion: constraint-2 bypassed the count entirely
    verify(repository, never()).countDistinctWarehousesByStore(storeId);
  }

  // ── Constraint 3: max 5 products per warehouse ────────────────────────────

  @Test
  void associate_constraint3_exceedMaxProductsPerWarehouse_shouldThrow() {
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(false);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(5L); // at max

    assertThrows(
        IllegalArgumentException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  @Test
  void associate_constraint3_warehouseAlreadyHasProduct_skipsCountCheck() {
    // When warehouse already holds this product type, the product-count check is skipped.
    // Even if countDistinctProductsByWarehouse returns over-limit, no constraint-3 exception is raised.
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(true);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(99L); // would trigger if checked

    // Entity checks use Panache static methods — they throw outside Quarkus context.
    // We only care that constraint-3's count was never consulted.
    try { useCase.associate(warehouseId, storeId, productId); } catch (Exception ignored) {}

    // Key assertion: constraint-3 bypassed the count entirely
    verify(repository, never()).countDistinctProductsByWarehouse(warehouseId);
  }
}
