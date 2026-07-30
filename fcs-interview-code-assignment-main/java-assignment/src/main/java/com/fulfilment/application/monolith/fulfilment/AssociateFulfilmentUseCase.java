package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
  @Inject ProductRepository productRepository;

  public FulfilmentAssociation associate(Long warehouseId, Long storeId, Long productId) {

    // Guard: reject exact duplicate triple before any constraint checks
    if (repository.associationExists(warehouseId, storeId, productId)) {
      throw new IllegalArgumentException(
          "Association (warehouse=" + warehouseId + ", store=" + storeId
              + ", product=" + productId + ") already exists.");
    }

    // Constraint 1: max 2 warehouses fulfilling the same product to the same store
    long warehousesForProductAtStore =
        repository.countDistinctWarehousesForProductAtStore(storeId, productId);
    if (warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new IllegalArgumentException(
          "Product " + productId + " is already fulfilled by "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses at store " + storeId + ".");
    }

    // Constraint 2: max 3 warehouses serving the same store
    // Only count this warehouseId if it is new to this store
    if (!repository.warehouseAlreadyServesStore(warehouseId, storeId)) {
      long warehousesAtStore = repository.countDistinctWarehousesByStore(storeId);
      if (warehousesAtStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new IllegalArgumentException(
            "Store " + storeId + " is already fulfilled by "
                + MAX_WAREHOUSES_PER_STORE + " warehouses.");
      }
    }

    // Constraint 3: max 5 product types per warehouse
    // Only count this productId if it is new to this warehouse
    if (!repository.warehouseAlreadyHasProduct(warehouseId, productId)) {
      long productsInWarehouse = repository.countDistinctProductsByWarehouse(warehouseId);
      if (productsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new IllegalArgumentException(
            "Warehouse " + warehouseId + " already stores "
                + MAX_PRODUCTS_PER_WAREHOUSE + " types of products.");
      }
    }

    // Validate referenced entities exist (DB check, done after constraint checks)
    if (warehouseRepository.findById(warehouseId) == null) {
      throw new IllegalArgumentException("Warehouse with id '" + warehouseId + "' not found.");
    }
    if (Store.findById(storeId) == null) {
      throw new IllegalArgumentException("Store with id '" + storeId + "' not found.");
    }
    if (productRepository.findById(productId) == null) {
      throw new IllegalArgumentException("Product with id '" + productId + "' not found.");
    }

    FulfilmentAssociation association =
        new FulfilmentAssociation(warehouseId, storeId, productId);
    repository.persist(association);
    return association;
  }
}
