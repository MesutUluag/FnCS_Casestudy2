package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

/**
 * Enforces the three fulfilment association constraints before persisting a new association:
 *
 * <ol>
 *   <li>Each Product can be fulfilled by at most 2 different Warehouses per Store.
 *   <li>Each Store can be fulfilled by at most 3 different Warehouses.
 *   <li>Each Warehouse can store at most 5 types of Products.
 * </ol>
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

    // Step 2: reject exact duplicate triple
    if (repository.associationExists(warehouseId, storeId, productId)) {
      throw new IllegalArgumentException(
          "Association (warehouse=" + warehouseId + ", store=" + storeId
              + ", product=" + productId + ") already exists.");
    }

    // Step 3: Constraint 1 — max 2 warehouses fulfilling the same product to the same store
    long warehousesForProductAtStore =
        repository.countDistinctWarehousesForProductAtStore(storeId, productId);
    if (warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new IllegalArgumentException(
          "Product " + productId + " is already fulfilled by "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses at store " + storeId + ".");
    }

    // Step 4: Constraint 2 — max 3 warehouses serving the same store
    // Only count this warehouseId if it is new to this store
    if (!repository.warehouseAlreadyServesStore(warehouseId, storeId)) {
      long warehousesAtStore = repository.countDistinctWarehousesByStore(storeId);
      if (warehousesAtStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new IllegalArgumentException(
            "Store " + storeId + " is already fulfilled by "
                + MAX_WAREHOUSES_PER_STORE + " warehouses.");
      }
    }

    // Step 5: Constraint 3 — max 5 product types per warehouse
    // Only count this productId if it is new to this warehouse
    if (!repository.warehouseAlreadyHasProduct(warehouseId, productId)) {
      long productsInWarehouse = repository.countDistinctProductsByWarehouse(warehouseId);
      if (productsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new IllegalArgumentException(
            "Warehouse " + warehouseId + " already stores "
                + MAX_PRODUCTS_PER_WAREHOUSE + " types of products.");
      }
    }

    FulfilmentAssociation association =
        new FulfilmentAssociation(warehouseId, storeId, productId);
    repository.persist(association);
    return association;
  }
}
