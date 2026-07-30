package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Observes StoreEvents AFTER the transaction has successfully committed, then propagates changes to
 * the legacy system. This guarantees the legacy system only receives data that has been durably
 * stored in our database.
 */
@ApplicationScoped
public class StoreEventObserver {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreEvent(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
    switch (event.type) {
      case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(event.store);
      case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store);
    }
  }
}
