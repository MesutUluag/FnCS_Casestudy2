package com.fulfilment.application.monolith.fulfilment;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for Warehouse-Product-Store fulfilment associations.
 *
 * <p>POST /fulfilment — create a new association (enforces all 3 constraints)
 *
 * <p>GET /fulfilment — list all associations
 */
@Path("fulfilment")
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  @Inject AssociateFulfilmentUseCase useCase;
  @Inject FulfilmentAssociationRepository repository;

  @GET
  public List<FulfilmentAssociation> list() {
    return repository.listAssociations();
  }

  @POST
  @Transactional
  public Response associate(FulfilmentRequest request) {
    if (request.warehouseId == null || request.storeId == null || request.productId == null) {
      throw new WebApplicationException("warehouseId, storeId and productId are all required.", 400);
    }
    try {
      FulfilmentAssociation association =
          useCase.associate(request.warehouseId, request.storeId, request.productId);
      return Response.status(201).entity(association).build();
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

}
