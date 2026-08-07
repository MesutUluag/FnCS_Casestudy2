package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class LocationGateway implements LocationResolver {

  private static final List<Location> LOCATIONS = List.of(
      new Location("ZWOLLE-001", 1, 40),
      new Location("ZWOLLE-002", 2, 50),
      new Location("AMSTERDAM-001", 5, 100),
      new Location("AMSTERDAM-002", 3, 75),
      new Location("TILBURG-001", 1, 40),
      new Location("HELMOND-001", 1, 45),
      new Location("EINDHOVEN-001", 2, 70),
      new Location("VETSBY-001", 1, 90)
  );

  @Override
  public Location resolveByIdentifier(String identifier) {
    return LOCATIONS.stream()
        .filter(location -> location.identification.equals(identifier))
        .findFirst()
        .orElse(null);
  }
}
