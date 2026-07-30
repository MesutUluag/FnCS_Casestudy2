package com.fulfilment.application.monolith.stores;

/** Event fired when a Store is created or updated, to be observed after transaction commits. */
public class StoreEvent {

  public enum Type {
    CREATED,
    UPDATED
  }

  public final Store store;
  public final Type type;

  public StoreEvent(Store store, Type type) {
    this.store = store;
    this.type = type;
  }
}
