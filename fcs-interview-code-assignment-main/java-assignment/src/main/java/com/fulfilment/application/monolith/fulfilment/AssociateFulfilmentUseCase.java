package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.fulfilment.FulfilmentConstraintException.Constraint;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

/**
 * Enforces the four fulfilment association rules before persisting a new association:
 *
 * <ol>
 *   <li>Duplicate guard — the exact (warehouse, store, product) triple must be unique.
 *   <li>Each Product can be supplied to one Store by at most 2 different Warehouses.
 *   <li>Each Store can be served by at most 3 different Warehouses (across all products).
 *   <li>Each Warehouse can hold at most 5 distinct product types.
 * </ol>
 *
 * <p>Violations are reported as {@link FulfilmentConstraintException} so the REST layer can
 * return a structured, human-readable 422 response to the frontend.
 */
@ApplicationScoped
public class AssociateFulfilmentUseCase {

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject FulfilmentAssociationRepository repository;
  @Inject WarehouseRepository warehouseRepository;
  @Inject StoreRepository storeRepository;
  @Inject ProductRepository productRepository;

  public FulfilmentAssociation associate(Long warehouseId, Long storeId, Long productId) {

    // Step 1: validate that all referenced entities exist before any other check.
    // A missing entity must return 404 immediately — not a misleading constraint error.
    if (warehouseRepository.findById(warehouseId) == null) {
      throw new NotFoundException("Warehouse with id '" + warehouseId + "' not found.");
    }
    if (storeRepository.findById(storeId) == null) {
      throw new NotFoundException("Store with id '" + storeId + "' not found.");
    }
    if (productRepository.findById(productId) == null) {
      throw new NotFoundException("Product with id '" + productId + "' not found.");
    }

    // Step 2: Duplicate guard — (warehouse, store, product) triple must be unique
    if (repository.associationExists(warehouseId, storeId, productId)) {
      throw new FulfilmentConstraintException(
          Constraint.DUPLICATE_ASSOCIATION,
          "This warehouse is already assigned to supply this product to the selected store. "
              + "Each warehouse-store-product combination must be unique.");
    }

    // Step 3: Constraint 1 — a product can be supplied to one store by at most 2 warehouses
    long warehousesForProductAtStore =
        repository.countDistinctWarehousesForProductAtStore(storeId, productId);
    if (warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new FulfilmentConstraintException(
          Constraint.MAX_WAREHOUSES_PER_PRODUCT_PER_STORE,
          "This product is already being supplied to the selected store by "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses, which is the maximum allowed. "
              + "Please remove an existing warehouse assignment before adding a new one.");
    }

    // Step 4: Constraint 2 — a store can be served by at most 3 warehouses (across all products)
    // Only count this warehouseId if it is new to this store
    if (!repository.warehouseAlreadyServesStore(warehouseId, storeId)) {
      long warehousesAtStore = repository.countDistinctWarehousesByStore(storeId);
      if (warehousesAtStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new FulfilmentConstraintException(
            Constraint.MAX_WAREHOUSES_PER_STORE,
            "The selected store is already served by "
                + MAX_WAREHOUSES_PER_STORE
                + " warehouses, which is the maximum allowed. "
                + "Please remove an existing warehouse before assigning a new one to this store.");
      }
    }

    // Step 5: Constraint 3 — a warehouse can hold at most 5 distinct product types
    // Only count this productId if it is new to this warehouse
    if (!repository.warehouseAlreadyHasProduct(warehouseId, productId)) {
      long productsInWarehouse = repository.countDistinctProductsByWarehouse(warehouseId);
      if (productsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new FulfilmentConstraintException(
            Constraint.MAX_PRODUCTS_PER_WAREHOUSE,
            "The selected warehouse already holds "
                + MAX_PRODUCTS_PER_WAREHOUSE
                + " distinct product types, which is the maximum allowed. "
                + "Please remove a product from this warehouse before adding a new one.");
      }
    }

    FulfilmentAssociation association =
        new FulfilmentAssociation(warehouseId, storeId, productId);
    repository.persist(association);
    return association;
  }
}
