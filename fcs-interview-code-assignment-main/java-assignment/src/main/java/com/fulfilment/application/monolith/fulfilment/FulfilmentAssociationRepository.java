package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfilmentAssociationRepository
    implements PanacheRepository<FulfilmentAssociation> {

  /**
   * Number of distinct warehouses fulfilling {@code productId} at {@code storeId} (constraint 1).
   * Uses a single COUNT(DISTINCT) query — no row fetch, no in-memory distinct.
   */
  public long countDistinctWarehousesForProductAtStore(Long storeId, Long productId) {
    return (long) getEntityManager()
        .createQuery(
            "select count(distinct a.warehouseId) from FulfilmentAssociation a"
                + " where a.storeId = :storeId and a.productId = :productId")
        .setParameter("storeId", storeId)
        .setParameter("productId", productId)
        .getSingleResult();
  }

  /**
   * Number of distinct warehouses serving {@code storeId} across all products (constraint 2).
   * Uses a single COUNT(DISTINCT) query — no row fetch, no in-memory distinct.
   */
  public long countDistinctWarehousesByStore(Long storeId) {
    return (long) getEntityManager()
        .createQuery(
            "select count(distinct a.warehouseId) from FulfilmentAssociation a"
                + " where a.storeId = :storeId")
        .setParameter("storeId", storeId)
        .getSingleResult();
  }

  /** Whether this warehouse already serves this store (for constraint 2 new-warehouse check). */
  public boolean warehouseAlreadyServesStore(Long warehouseId, Long storeId) {
    return count("warehouseId = ?1 and storeId = ?2", warehouseId, storeId) > 0;
  }

  /**
   * Number of distinct product types held by {@code warehouseId} (constraint 3).
   * Uses a single COUNT(DISTINCT) query — no row fetch, no in-memory distinct.
   */
  public long countDistinctProductsByWarehouse(Long warehouseId) {
    return (long) getEntityManager()
        .createQuery(
            "select count(distinct a.productId) from FulfilmentAssociation a"
                + " where a.warehouseId = :warehouseId")
        .setParameter("warehouseId", warehouseId)
        .getSingleResult();
  }

  /** Whether this warehouse already holds this product type (for constraint 3 new-product check). */
  public boolean warehouseAlreadyHasProduct(Long warehouseId, Long productId) {
    return count("warehouseId = ?1 and productId = ?2", warehouseId, productId) > 0;
  }

  /** Whether the exact (warehouseId, storeId, productId) triple already exists (duplicate guard). */
  public boolean associationExists(Long warehouseId, Long storeId, Long productId) {
    return count("warehouseId = ?1 and storeId = ?2 and productId = ?3",
        warehouseId, storeId, productId) > 0;
  }

  /** All associations (for listing). */
  public List<FulfilmentAssociation> listAssociations() {
    return listAll();
  }
}
