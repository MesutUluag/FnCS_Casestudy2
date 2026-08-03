package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehouseConstraintException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST adapter for warehouse operations. Constraint violations thrown by the use cases are caught
 * here and converted into HTTP 422 Unprocessable Entity with a structured JSON body:
 *
 * <pre>{@code
 * {
 *   "code": 422,
 *   "constraint": "DUPLICATE_BUSINESS_UNIT_CODE",
 *   "error": "A warehouse with business unit code 'MWH.001' already exists. ..."
 * }
 * }</pre>
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject private ObjectMapper objectMapper;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream()
        .filter(w -> w.archivedAt == null)
        .map(this::toWarehouseResponse)
        .toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toDomainWarehouse(data);
    try {
      createWarehouseOperation.create(warehouse);
    } catch (WarehouseConstraintException e) {
      throw new WebApplicationException(constraintResponse(e));
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = resolveWarehouseById(id);
    if (warehouse == null || warehouse.archivedAt != null) {
      throw new WebApplicationException("Warehouse with id '" + id + "' not found.", 404);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    var warehouse = resolveWarehouseById(id);
    if (warehouse == null || warehouse.archivedAt != null) {
      throw new WebApplicationException("Warehouse with id '" + id + "' not found.", 404);
    }
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var newWarehouse = toDomainWarehouse(data);
    newWarehouse.businessUnitCode = businessUnitCode;
    try {
      replaceWarehouseOperation.replace(newWarehouse);
    } catch (NoSuchElementException e) {
      throw new WebApplicationException(e.getMessage(), 404);
    } catch (WarehouseConstraintException e) {
      throw new WebApplicationException(constraintResponse(e));
    }
    return toWarehouseResponse(newWarehouse);
  }

  /** Builds a structured 422 JSON response body for a constraint violation. */
  private Response constraintResponse(WarehouseConstraintException e) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("code", 422);
    body.put("constraint", e.getConstraint().name());
    body.put("error", e.getMessage());
    return Response.status(422).entity(body).type("application/json").build();
  }

  /**
   * Resolves a warehouse by either its numeric DB id (e.g. "1") or its businessUnitCode (e.g.
   * "MWH.001").
   */
  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse resolveWarehouseById(
      String id) {
    try {
      long numericId = Long.parseLong(id);
      var dbWarehouse = warehouseRepository.findById(numericId);
      return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
    } catch (NumberFormatException e) {
      return warehouseRepository.findByBusinessUnitCode(id);
    }
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse apiBean) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = apiBean.getBusinessUnitCode();
    warehouse.location = apiBean.getLocation();
    warehouse.capacity = apiBean.getCapacity();
    warehouse.stock = apiBean.getStock();
    return warehouse;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    if (warehouse.id != null) {
      response.setId(String.valueOf(warehouse.id));
    }
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}
