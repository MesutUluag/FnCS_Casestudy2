package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.exception.DbConstraints;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Cacheable
@Table(
    name = "store",
    uniqueConstraints = @UniqueConstraint(name = DbConstraints.STORE_NAME, columnNames = "name"))
public class Store extends PanacheEntity {

  @Column(length = 40)
  public String name;

  public int quantityProductsInStock;

  public Store() {}

  public Store(String name) {
    this.name = name;
  }
}
