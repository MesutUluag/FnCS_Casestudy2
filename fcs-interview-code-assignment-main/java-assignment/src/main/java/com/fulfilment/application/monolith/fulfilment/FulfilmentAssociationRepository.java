package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfilmentAssociationRepository
    implements PanacheRepository<FulfilmentAssociation> {

  /** Distinct warehouse IDs that fulfil productId at storeId (constraint 1). */
  public long countDistinctWarehousesForProductAtStore(Long storeId, Long productId) {
    return list("storeId = ?1 and productId = ?2", storeId, productId).stream()
        .map(a -> a.warehouseId)
        .distinct()
        .count();
  }

  /** Distinct warehouse IDs serving storeId across all products (constraint 2). */
  public long countDistinctWarehousesByStore(Long storeId) {
    return list("storeId", storeId).stream()
        .map(a -> a.warehouseId)
        .distinct()
        .count();
  }

  /** Whether this warehouse already serves this store (for constraint 2 new-warehouse check). */
  public boolean warehouseAlreadyServesStore(Long warehouseId, Long storeId) {
    return count("warehouseId = ?1 and storeId = ?2", warehouseId, storeId) > 0;
  }

  /** Distinct product type IDs stored by warehouseId (constraint 3). */
  public long countDistinctProductsByWarehouse(Long warehouseId) {
    return list("warehouseId", warehouseId).stream()
        .map(a -> a.productId)
        .distinct()
        .count();
  }

  /** Whether this warehouse already holds this product type (for constraint 3 new-product check). */
  public boolean warehouseAlreadyHasProduct(Long warehouseId, Long productId) {
    return count("warehouseId = ?1 and productId = ?2", warehouseId, productId) > 0;
  }

  /** All associations (for listing). */
  public List<FulfilmentAssociation> listAssociations() {
    return listAll();
  }
}
