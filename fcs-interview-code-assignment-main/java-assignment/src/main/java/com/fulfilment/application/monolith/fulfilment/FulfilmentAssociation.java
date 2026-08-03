package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.exception.DbConstraints;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents the fulfilment association between a Warehouse, a Store and a Product. A Warehouse
 * acts as the fulfilment source for a specific Product delivered to a specific Store.
 *
 * <p>Constraints (enforced at use-case level):
 *
 * <ul>
 *   <li>Each Product can be fulfilled by at most 2 different Warehouses per Store.
 *   <li>Each Store can be fulfilled by at most 3 different Warehouses.
 *   <li>Each Warehouse can store at most 5 types of Products.
 * </ul>
 */
@Entity
@Cacheable
@Table(
    name = "fulfilment_association",
    uniqueConstraints =
        @UniqueConstraint(
            name = DbConstraints.FULFILMENT_ASSOCIATION_TRIPLE,
            columnNames = {"warehouseId", "storeId", "productId"}))
public class FulfilmentAssociation {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false)
  public Long warehouseId;

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false)
  public Long productId;

  public FulfilmentAssociation() {}

  public FulfilmentAssociation(Long warehouseId, Long storeId, Long productId) {
    this.warehouseId = warehouseId;
    this.storeId = storeId;
    this.productId = productId;
  }
}
