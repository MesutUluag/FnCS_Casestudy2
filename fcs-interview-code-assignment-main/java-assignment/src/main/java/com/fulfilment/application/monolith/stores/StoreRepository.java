package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository adapter for {@link Store}. Provides an injectable, mockable alternative to the
 * Panache active-record static methods on {@code Store}, enabling use-case unit testing without a
 * running Quarkus context.
 */
@ApplicationScoped
public class StoreRepository implements PanacheRepository<Store> {}
