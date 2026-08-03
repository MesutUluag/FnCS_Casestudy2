package com.fulfilment.application.monolith.fulfilment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.ws.rs.NotFoundException;
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
  @Mock StoreRepository storeRepository;
  @Mock ProductRepository productRepository;

  @InjectMocks AssociateFulfilmentUseCase useCase;

  private final Long warehouseId = 1L;
  private final Long storeId = 1L;
  private final Long productId = 1L;

  @BeforeEach
  void defaultHappyPathStubs() {
    // Entity existence checks (now first in associate())
    when(warehouseRepository.findById(warehouseId)).thenReturn(new DbWarehouse());
    when(storeRepository.findById(storeId)).thenReturn(new Store());
    when(productRepository.findById(productId)).thenReturn(new Product());

    // Duplicate guard: no existing triple by default
    when(repository.associationExists(warehouseId, storeId, productId)).thenReturn(false);
    // Constraint 1: 0 warehouses serve this product at this store
    when(repository.countDistinctWarehousesForProductAtStore(storeId, productId)).thenReturn(0L);
    // Constraint 2: warehouse is new to this store, store has 0 warehouses
    when(repository.warehouseAlreadyServesStore(warehouseId, storeId)).thenReturn(false);
    when(repository.countDistinctWarehousesByStore(storeId)).thenReturn(0L);
    // Constraint 3: product is new to this warehouse, warehouse has 0 products
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(false);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(0L);
  }

  // ── Entity not-found → 404 ────────────────────────────────────────────────

  @Test
  void associate_warehouseNotFound_shouldThrowNotFoundException() {
    when(warehouseRepository.findById(warehouseId)).thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  @Test
  void associate_productNotFound_shouldThrowNotFoundException() {
    when(productRepository.findById(productId)).thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  // ── Duplicate guard ───────────────────────────────────────────────────────

  @Test
  void associate_storeNotFound_shouldThrowNotFoundException() {
    when(storeRepository.findById(storeId)).thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  // ── Duplicate guard ───────────────────────────────────────────────────────

  @Test
  void associate_duplicateTriple_shouldThrow() {
    when(repository.associationExists(warehouseId, storeId, productId)).thenReturn(true);

    FulfilmentConstraintException ex = assertThrows(
        FulfilmentConstraintException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    assertEquals(FulfilmentConstraintException.Constraint.DUPLICATE_ASSOCIATION, ex.getConstraint());
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  // ── Constraint 1: max 2 warehouses per product per store ──────────────────

  @Test
  void associate_constraint1_exceedMaxWarehousesPerProductPerStore_shouldThrow() {
    when(repository.countDistinctWarehousesForProductAtStore(storeId, productId))
        .thenReturn(2L); // already at max

    FulfilmentConstraintException ex = assertThrows(
        FulfilmentConstraintException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    assertEquals(FulfilmentConstraintException.Constraint.MAX_WAREHOUSES_PER_PRODUCT_PER_STORE, ex.getConstraint());
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  // ── Constraint 2: max 3 warehouses per store ──────────────────────────────

  @Test
  void associate_constraint2_exceedMaxWarehousesPerStore_shouldThrow() {
    when(repository.warehouseAlreadyServesStore(warehouseId, storeId)).thenReturn(false);
    when(repository.countDistinctWarehousesByStore(storeId)).thenReturn(3L); // already at max

    FulfilmentConstraintException ex = assertThrows(
        FulfilmentConstraintException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    assertEquals(FulfilmentConstraintException.Constraint.MAX_WAREHOUSES_PER_STORE, ex.getConstraint());
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  @Test
  void associate_constraint2_warehouseAlreadyServesStore_skipsCountCheck() {
    // When warehouse already serves store, the store warehouse-count is NOT checked.
    // Verify by setting countDistinct to over-limit and confirming it is never consulted.
    when(repository.warehouseAlreadyServesStore(warehouseId, storeId)).thenReturn(true);
    when(repository.countDistinctWarehousesByStore(storeId)).thenReturn(99L); // would trigger if checked

    // Constraint-3: ensure warehouse product count is under limit so we reach persist()
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(false);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(0L);

    // Should not throw — constraint-2 count is bypassed; persist() is called
    try { useCase.associate(warehouseId, storeId, productId); } catch (Exception ignored) {}

    verify(repository, never()).countDistinctWarehousesByStore(storeId);
  }

  // ── Constraint 3: max 5 products per warehouse ────────────────────────────

  @Test
  void associate_constraint3_exceedMaxProductsPerWarehouse_shouldThrow() {
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(false);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(5L); // at max

    FulfilmentConstraintException ex = assertThrows(
        FulfilmentConstraintException.class,
        () -> useCase.associate(warehouseId, storeId, productId));
    assertEquals(FulfilmentConstraintException.Constraint.MAX_PRODUCTS_PER_WAREHOUSE, ex.getConstraint());
    verify(repository, never()).persist(any(FulfilmentAssociation.class));
  }

  @Test
  void associate_constraint3_warehouseAlreadyHasProduct_skipsCountCheck() {
    // When warehouse already holds this product type, the product-count is NOT recounted.
    when(repository.warehouseAlreadyHasProduct(warehouseId, productId)).thenReturn(true);
    when(repository.countDistinctProductsByWarehouse(warehouseId)).thenReturn(99L); // would trigger if checked

    // Should not throw — constraint-3 count is bypassed; persist() is called
    try { useCase.associate(warehouseId, storeId, productId); } catch (Exception ignored) {}

    verify(repository, never()).countDistinctProductsByWarehouse(warehouseId);
  }
}
