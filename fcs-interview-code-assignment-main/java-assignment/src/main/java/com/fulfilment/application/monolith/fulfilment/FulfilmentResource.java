package com.fulfilment.application.monolith.fulfilment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * <p>POST /fulfilment — create a new association (enforces all 4 rules)
 *
 * <p>GET /fulfilment — list all associations
 *
 * <p>Constraint violations are returned as HTTP 422 Unprocessable Entity with a structured JSON
 * body so that frontend consumers can display a human-readable explanation:
 *
 * <pre>{@code
 * {
 *   "code": 422,
 *   "constraint": "MAX_WAREHOUSES_PER_STORE",
 *   "error": "The selected store is already served by 3 warehouses …"
 * }
 * }</pre>
 */
@Path("fulfilment")
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  @Inject AssociateFulfilmentUseCase useCase;
  @Inject FulfilmentAssociationRepository repository;
  @Inject ObjectMapper objectMapper;

  @GET
  public List<FulfilmentAssociation> list() {
    return repository.listAssociations();
  }

  @POST
  @Transactional
  public Response associate(FulfilmentRequest request) {
    if (request == null
        || request.warehouseId == null
        || request.storeId == null
        || request.productId == null) {
      throw new WebApplicationException("warehouseId, storeId and productId are all required.", 400);
    }
    try {
      FulfilmentAssociation association =
          useCase.associate(request.warehouseId, request.storeId, request.productId);
      return Response.status(201).entity(association).build();
    } catch (FulfilmentConstraintException e) {
      ObjectNode body = objectMapper.createObjectNode();
      body.put("code", 422);
      body.put("constraint", e.getConstraint().name());
      body.put("error", e.getMessage());
      return Response.status(422).entity(body).type("application/json").build();
    }
  }
}
