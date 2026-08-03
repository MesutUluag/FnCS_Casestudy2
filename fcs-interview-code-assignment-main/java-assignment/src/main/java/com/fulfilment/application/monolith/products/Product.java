package com.fulfilment.application.monolith.products;

import com.fulfilment.application.monolith.exception.DbConstraints;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Cacheable
@Table(
    name = "product",
    uniqueConstraints = @UniqueConstraint(name = DbConstraints.PRODUCT_NAME, columnNames = "name"))
public class Product {

  @Id @GeneratedValue public Long id;

  @Column(length = 40)
  public String name;

  @Column(nullable = true)
  public String description;

  @Column(precision = 10, scale = 2, nullable = true)
  public BigDecimal price;

  public int stock;

  public Product() {}

  public Product(String name) {
    this.name = name;
  }
}
