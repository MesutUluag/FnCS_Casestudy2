# FnCS Case Study 2 — Completion Plan

## Top-Level Overview

This project is an interview case study with two deliverables:

1. **Java Code Assignment** — A Quarkus 3.13.3 / Java 17 / PostgreSQL application implementing a fulfillment domain (Locations, Stores, Warehouses, Products). Several core methods are stubbed out with `UnsupportedOperationException` and need to be implemented following a hexagonal (ports & adapters) architecture. Three "Must-Have" tasks and one Bonus task are required.

2. **Written Deliverables** — Three questions in `QUESTIONS.md` and a case-study discussion in `CASE_STUDY.md` need written answers.

The plan is ordered so that each sub-task can be reviewed independently before the next begins.

---

## Sub-Tasks

---

### Sub-Task 1 — Implement `LocationGateway.resolveByIdentifier()`

**Status:** `[ ] pending`

**Intent:**
This is the warm-up task and a pre-requisite for all warehouse operations. The static list of `Location` objects already exists inside the class. The single method `resolveByIdentifier` just needs to search that list and return the matching `Location` or `null`.

**Expected Outcomes:**
- `LocationGateway.resolveByIdentifier("ZWOLLE-001")` returns the corresponding `Location` object.
- `LocationGateway.resolveByIdentifier("UNKNOWN")` returns `null` (or throws, per validation logic in use cases).
- `LocationGatewayTest` is enabled (assertions uncommented) and passes.

**Todo List:**
1. Open [`LocationGateway.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java).
2. Replace the `throw new UnsupportedOperationException()` in `resolveByIdentifier` with a stream filter on the static `locations` list that matches on `identification` field, returning the first match or `null`.
3. Open [`LocationGatewayTest.java`](fcs-interview-code-assignment-main/java-assignment/src/test/java/com/fulfilment/application/monolith/location/LocationGatewayTest.java) and uncomment the disabled assertions to activate the test.

**Relevant Context:**
- [`LocationGateway.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java) — Implements `LocationResolver` port; has a static `List<Location>` with 8 entries.
- [`LocationResolver.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/ports/LocationResolver.java) — Interface with single method `Location resolveByIdentifier(String identifier)`.
- [`Location.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/models/Location.java) — Domain model with `identification`, `maxNumberOfWarehouses`, `maxCapacity` fields.

---

### Sub-Task 2 — Fix `StoreResource` Transaction Ordering

**Status:** `[ ] pending`

**Intent:**
The `StoreResource` currently calls `LegacyStoreManagerGateway` (which simulates a downstream legacy system sync) *inside* the same `@Transactional` method as the DB persist, before the transaction commits. This means the legacy system could receive a notification about data that hasn't been committed yet (and might be rolled back). The fix ensures the legacy call only happens *after* the database commit is confirmed.

The recommended Quarkus approach is to use a CDI observer with `@Observes(during = TransactionPhase.AFTER_SUCCESS)` on a transactional event, or to split the work so the DB commit happens in a method annotated with `@Transactional(REQUIRES_NEW)` before the gateway call. The simplest pattern for Quarkus is to use the **TransactionSynchronization** mechanism or emit a CDI event that is only observed after commit.

**Expected Outcomes:**
- `POST /store`, `PUT /store/{id}`, `PATCH /store/{id}` all write to DB first, commit, then call the legacy gateway.
- If the DB write fails, the legacy gateway is never called.
- Existing behaviour for `GET` and `DELETE` is unchanged.

**Todo List:**
1. Open [`StoreResource.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/stores/StoreResource.java) and study the three mutating methods (POST, PUT, PATCH).
2. Extract the DB-write portion of each method into a helper annotated `@Transactional` so it commits before returning.
3. Call the legacy gateway *after* the helper returns — ensuring the DB operation has already committed.
   - Alternatively, fire a CDI event from within the transaction and observe it `@Observes(during = TransactionPhase.AFTER_SUCCESS)` in a separate bean that calls the legacy gateway. Pick whichever approach is cleaner given the existing code style.

**Relevant Context:**
- [`StoreResource.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/stores/StoreResource.java) — JAX-RS resource; methods annotated `@Transactional`.
- [`LegacyStoreManagerGateway.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/stores/LegacyStoreManagerGateway.java) — Writes to temp files as simulation; no transaction awareness.
- [`Store.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/stores/Store.java) — `PanacheEntity` with `name` and `quantityProductsInStock`.

---

### Sub-Task 3 — Implement `WarehouseRepository` (Persistence Adapter)

**Status:** `[ ] pending`

**Intent:**
`WarehouseRepository` is the adapter between the domain and the database. It implements the `WarehouseStore` port. Currently four of its five methods throw `UnsupportedOperationException`. This sub-task fills those in so all downstream use cases can function.

**Expected Outcomes:**
- `create(Warehouse)` — maps domain `Warehouse` to `DbWarehouse` and persists.
- `update(Warehouse)` — finds existing `DbWarehouse` by `businessUnitCode`, updates its fields and merges.
- `remove(Warehouse)` — finds and deletes the `DbWarehouse` by `businessUnitCode`.
- `findByBusinessUnitCode(String)` — queries by `businessUnitCode`, maps to domain `Warehouse` and returns it (or `null`).

**Todo List:**
1. Open [`WarehouseRepository.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/adapters/database/WarehouseRepository.java).
2. Implement `create(Warehouse)`: instantiate `DbWarehouse`, copy all fields from domain model, call `persist()`.
3. Implement `findByBusinessUnitCode(String)`: use Panache's `find("businessUnitCode", code).firstResult()`, map result via `DbWarehouse.toWarehouse()`.
4. Implement `update(Warehouse)`: retrieve existing entity by `businessUnitCode`, copy fields, let Panache dirty-check or call `persist()`.
5. Implement `remove(Warehouse)`: retrieve entity by `businessUnitCode`, call `delete()` on it.

**Relevant Context:**
- [`WarehouseRepository.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/adapters/database/WarehouseRepository.java) — Extends `PanacheRepository<DbWarehouse>`, implements `WarehouseStore`.
- [`DbWarehouse.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/adapters/database/DbWarehouse.java) — JPA entity; has `toWarehouse()` conversion method.
- [`Warehouse.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/models/Warehouse.java) — Domain model (POJO): `businessUnitCode`, `location`, `capacity`, `stock`, `createdAt`, `archivedAt`.
- [`WarehouseStore.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/ports/WarehouseStore.java) — Port interface being implemented.

---

### Sub-Task 4 — Implement Warehouse Use Cases

**Status:** `[ ] pending`

**Intent:**
The three use case classes (`CreateWarehouseUseCase`, `ArchiveWarehouseUseCase`, `ReplaceWarehouseUseCase`) each implement a domain port and orchestrate business logic. They are the heart of Task 3 in the assignment. Currently they contain no real logic.

**Expected Outcomes:**

*`CreateWarehouseUseCase`*:
- Rejects creation if `businessUnitCode` already exists → 409 or validation exception.
- Rejects if location cannot be resolved → 400.
- Rejects if location already has `maxNumberOfWarehouses` active warehouses → 400.
- Rejects if `capacity > location.maxCapacity` → 400.
- Rejects if `stock > capacity` → 400.
- On success: sets `createdAt = now()`, persists via `WarehouseStore.create()`.

*`ArchiveWarehouseUseCase`*:
- Sets `warehouse.archivedAt = now()` on the target warehouse.
- Calls `WarehouseStore.update()` to persist the archive timestamp.

*`ReplaceWarehouseUseCase`*:
- Finds existing warehouse by `businessUnitCode`.
- Validates new warehouse `capacity >= existing.stock` (accommodation check).
- Validates `newWarehouse.stock == existingWarehouse.stock` (stock matching).
- Archives old warehouse (sets `archivedAt`), calls `update`.
- Creates new warehouse with same `businessUnitCode`, calls `create`.

**Todo List:**
1. Open [`CreateWarehouseUseCase.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/CreateWarehouseUseCase.java).
   - Inject `LocationResolver` and `WarehouseStore` (already present via constructor or `@Inject`).
   - Add all five validation checks, throwing `IllegalArgumentException` or a dedicated exception type for each failure.
   - Set `warehouse.setCreatedAt(LocalDateTime.now())` before calling `warehouseStore.create(warehouse)`.
2. Open [`ArchiveWarehouseUseCase.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ArchiveWarehouseUseCase.java).
   - Set `warehouse.setArchivedAt(LocalDateTime.now())`.
   - Call `warehouseStore.update(warehouse)`.
3. Open [`ReplaceWarehouseUseCase.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ReplaceWarehouseUseCase.java).
   - Implement the full replace flow as described above.
4. Write unit tests in [`CreateWarehouseUseCaseTest.java`](fcs-interview-code-assignment-main/java-assignment/src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/CreateWarehouseUseCaseTest.java), [`ArchiveWarehouseUseCaseTest.java`](fcs-interview-code-assignment-main/java-assignment/src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ArchiveWarehouseUseCaseTest.java), and [`ReplaceWarehouseUseCaseTest.java`](fcs-interview-code-assignment-main/java-assignment/src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ReplaceWarehouseUseCaseTest.java) — using Mockito to mock ports and verifying both happy-path and each validation failure.

**Relevant Context:**
- [`CreateWarehouseUseCase.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/CreateWarehouseUseCase.java)
- [`ArchiveWarehouseUseCase.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ArchiveWarehouseUseCase.java)
- [`ReplaceWarehouseUseCase.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ReplaceWarehouseUseCase.java)
- [`Warehouse.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/models/Warehouse.java)
- [`Location.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/models/Location.java)
- [`import.sql`](fcs-interview-code-assignment-main/java-assignment/src/main/resources/import.sql) — defines existing location data and initial warehouse seed data.

---

### Sub-Task 5 — Implement `WarehouseResourceImpl` (REST Adapter)

**Status:** `[ ] pending`

**Intent:**
`WarehouseResourceImpl` is the REST adapter that implements the auto-generated `WarehouseResource` interface (from `warehouse-openapi.yaml`). The `listAllWarehousesUnits()` method already works. The remaining four endpoint handlers need to delegate to use cases, convert between API beans and domain models, and map exceptions to appropriate HTTP status codes.

**Expected Outcomes:**
- `POST /warehouse` → calls `CreateWarehouseOperation.create()` → 201 Created on success, 400/409 on validation failure.
- `GET /warehouse/{id}` → looks up warehouse by businessUnitCode, returns 200 with body or 404.
- `DELETE /warehouse/{id}` → looks up warehouse by businessUnitCode, calls `ArchiveWarehouseOperation.archive()` → 204 on success, 404 if not found.
- `POST /warehouse/{businessUnitCode}/replacement` → calls `ReplaceWarehouseOperation.replace()` → 200 or 201 on success, 400/404 on error.
- `WarehouseEndpointIT` archived-warehouse test uncommented and passing.

**Todo List:**
1. Open [`WarehouseResourceImpl.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseResourceImpl.java).
2. Inject `CreateWarehouseOperation`, `ArchiveWarehouseOperation`, `ReplaceWarehouseOperation`, and `WarehouseStore` (for lookups).
3. Implement `createANewWarehouseUnit(Warehouse apiBean)`:
   - Convert API bean to domain `Warehouse`.
   - Call `createOperation.create(domain)`.
   - Return 201 or appropriate error response.
4. Implement `getAWarehouseUnitByID(String id)`:
   - Call `warehouseStore.findByBusinessUnitCode(id)`.
   - Return 200 with `toWarehouseResponse()` result, or 404.
5. Implement `archiveAWarehouseUnitByID(String id)`:
   - Find warehouse by `id`.
   - Call `archiveOperation.archive(warehouse)`.
   - Return 204, or 404.
6. Implement `replaceTheCurrentActiveWarehouse(String businessUnitCode, Warehouse apiBean)`:
   - Convert API bean to domain `Warehouse`.
   - Call `replaceOperation.replace(domain)`.
   - Return 200, or appropriate error.
7. Add a JAX-RS `ExceptionMapper` (or use `@ServerExceptionMapper`) to convert `IllegalArgumentException` → 400 and `NoSuchElementException` → 404.
8. Uncomment the disabled test in [`WarehouseEndpointIT.java`](fcs-interview-code-assignment-main/java-assignment/src/test/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseEndpointIT.java).

**Relevant Context:**
- [`WarehouseResourceImpl.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseResourceImpl.java)
- [`warehouse-openapi.yaml`](fcs-interview-code-assignment-main/java-assignment/src/main/resources/openapi/warehouse-openapi.yaml) — defines all endpoint signatures.
- [`WarehouseEndpointIT.java`](fcs-interview-code-assignment-main/java-assignment/src/test/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseEndpointIT.java) — Integration test using `@QuarkusIntegrationTest`.

---

### Sub-Task 6 — Answer `QUESTIONS.md`

**Status:** `[ ] pending`

**Intent:**
Provide written answers to the three technical discussion questions. Answers should be thoughtful, practical, and demonstrate senior-level reasoning about trade-offs.

**Expected Outcomes:**
- All three `Answer:` blocks in `QUESTIONS.md` filled with substantive responses.

**Todo List:**
1. Open [`QUESTIONS.md`](fcs-interview-code-assignment-main/java-assignment/QUESTIONS.md).
2. **Q1 — DB Access Layer Strategy:**
   - Observe that `Product` uses `PanacheRepository` directly; `Warehouse` uses a full adapter that implements a domain port (hexagonal); `Store` uses `PanacheEntity` (active-record pattern).
   - Discuss trade-offs: Panache active-record is concise but couples domain model to persistence; repository pattern separates concerns but adds boilerplate; the warehouse hexagonal approach is cleanest for testability and business rule enforcement.
   - State a preference: standardise on the hexagonal `WarehouseStore`-style for business-critical entities, keeping Panache active-record only for simple CRUD entities without complex rules.
3. **Q2 — OpenAPI Spec vs Direct Coding:**
   - Pros of spec-first (Warehouse): single source of truth, client generation, contract validation, easier API evolution.
   - Cons of spec-first: more setup, YAML can drift, generated code less idiomatic.
   - Pros of code-first (Product/Store): faster to start, idiomatic code, easier to evolve in small teams.
   - Cons: no machine-readable contract unless tooling generates it retrospectively.
   - State a preference (spec-first for public/cross-team APIs; code-first for internal/simple).
4. **Q3 — Test Prioritisation:**
   - Given constraints, prioritise use-case unit tests (pure Java, fast, high coverage of business logic) first.
   - Integration tests (`@QuarkusTest`) second — cover the REST adapter and DB interactions.
   - Avoid over-testing infrastructure code (repositories, mappers).
   - Strategy for maintaining coverage: enforce coverage gate in CI, add a test for every bug fix.

**Relevant Context:**
- [`QUESTIONS.md`](fcs-interview-code-assignment-main/java-assignment/QUESTIONS.md)

---

### Sub-Task 7 — Answer `CASE_STUDY.md` Scenarios

**Status:** `[ ] pending`

**Intent:**
Provide a high-level business/technical discussion for the 4 cost-control scenarios (the file already contains detailed sub-questions as prompts). The instructions say to bridge technical and business perspectives at a high level — no deep implementation required. Note: the existing `CASE_STUDY.md` already contains very detailed written content per scenario. Verify whether the candidate answers are already filled in, or if only the sub-question prompts exist with blank answer sections.

**Expected Outcomes:**
- Each scenario in [`CASE_STUDY.md`](fcs-interview-code-assignment-main/case-study/CASE_STUDY.md) has a written candidate answer addressing the business value and key technical considerations.

**Todo List:**
1. Open [`CASE_STUDY.md`](fcs-interview-code-assignment-main/case-study/CASE_STUDY.md) and check whether the candidate's answer sections are blank or filled.
2. If blank: write concise, business-oriented answers for each of the 4 scenarios (cost allocation, cost optimization, financial integration, budgeting/forecasting) that demonstrate understanding of the domain and bridge tech + business.
3. If already filled in with answers: confirm content is substantive and candidate-voiced, not just prompts.

**Relevant Context:**
- [`CASE_STUDY.md`](fcs-interview-code-assignment-main/case-study/CASE_STUDY.md)
- [`BRIEFING.md`](fcs-interview-code-assignment-main/case-study/BRIEFING.md)

---

### Sub-Task 8 — BONUS: Warehouse-Product-Store Associations

**Status:** `[ ] pending`

**Intent:**
Implement the optional feature: a `Warehouse` can be configured as a fulfilment source for a specific `Product` at a specific `Store`, subject to three constraints:
1. Max 2 warehouses per product per store.
2. Max 3 warehouses per store.
3. Max 5 product types per warehouse.

**Expected Outcomes:**
- New association entity/table exists (e.g. `WarehouseFulfilmentAssociation` mapping `warehouse`, `store`, `product`).
- A new endpoint or use case to create/manage these associations.
- All three constraints enforced and returning 400 on violation.
- At least basic test coverage for the constraint checks.

**Todo List:**
1. Design the association entity: `warehouse_id`, `store_id`, `product_id` — composite or surrogate key.
2. Add DB migration / import.sql additions.
3. Create a domain use case `AssociateFulfilmentUseCase` that enforces the three constraints.
4. Expose a REST endpoint (can be a simple `POST /fulfilment` with request body containing the three IDs).
5. Write tests for each constraint.

**Relevant Context:**
- [`Store.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/stores/Store.java)
- [`Product.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/products/Product.java)
- [`Warehouse.java`](fcs-interview-code-assignment-main/java-assignment/src/main/java/com/fulfilment/application/monolith/warehouses/domain/models/Warehouse.java)
- [`import.sql`](fcs-interview-code-assignment-main/java-assignment/src/main/resources/import.sql) — needs new table definition.

---

## Implementation Order

```
Sub-Task 1  →  Sub-Task 2  →  Sub-Task 3  →  Sub-Task 4  →  Sub-Task 5
   (Location)     (Store)        (Repository)   (Use Cases)   (REST layer)
        ↓
Sub-Task 6 + Sub-Task 7  (written answers — can be done any time)
        ↓
Sub-Task 8  (BONUS, after all must-haves pass)
```

Sub-Tasks 6 and 7 have no code dependencies and can be done in parallel with any of the above.
