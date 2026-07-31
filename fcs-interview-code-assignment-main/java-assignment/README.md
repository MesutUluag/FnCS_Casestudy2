# FnCS Case Study 2 — Java Assignment

A Quarkus monolith that implements a warehouse fulfilment management system.  
Exposes a REST API for managing **Warehouses**, **Stores**, **Products** and **Fulfilment Associations**, backed by PostgreSQL.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Architecture Overview](#architecture-overview)
3. [Business Rules](#business-rules)
4. [Project Structure](#project-structure)
5. [Getting Started](#getting-started)
6. [Running the Application](#running-the-application)
7. [API Reference](#api-reference)
8. [Running Tests](#running-tests)
9. [UI Dashboard](#ui-dashboard)
10. [Design Decisions](#design-decisions)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Quarkus 3.13.3 |
| REST | JAX-RS (Quarkus REST / RESTEasy Reactive) |
| ORM | Hibernate ORM with Panache |
| Database | PostgreSQL (Dev Services via Docker / Testcontainers) |
| Build | Maven Wrapper (`./mvnw`) |
| Testing | JUnit 5, Mockito (`quarkus-junit5-mockito`) |
| API Spec | OpenAPI 3.0 (spec-first for Warehouse; code-first for Store/Product) |

---

## Architecture Overview

The **Warehouse** domain follows a **hexagonal (ports & adapters)** architecture:

```
REST Layer (WarehouseResourceImpl)
    │
    ▼
Domain Ports (CreateWarehouseOperation, ArchiveWarehouseOperation, ReplaceWarehouseOperation)
    │
    ▼
Use Cases (CreateWarehouseUseCase, ArchiveWarehouseUseCase, ReplaceWarehouseUseCase)
    │
    ▼
Domain Port (WarehouseStore, LocationResolver)
    │
    ▼
Adapters (WarehouseRepository → PostgreSQL, LocationGateway → static config)
```

The **Store** and **Product** domains use a simpler Panache Repository pattern (no use-case layer).  
The **Fulfilment** domain follows the same hexagonal pattern as Warehouse (use case + repository adapter).

See [`QUESTIONS.md`](QUESTIONS.md) for a detailed discussion of the tradeoffs between these approaches.

---

## Business Rules

### Warehouses

| Rule | Detail |
|---|---|
| Unique BUC | No two active warehouses may share the same Business Unit Code. |
| Valid location | The warehouse location must exist in `LocationGateway` (static list). |
| Location warehouse limit | A location has a maximum number of warehouses it can host. |
| Capacity ≤ location max | Warehouse capacity cannot exceed the location's maximum capacity. |
| Stock ≤ capacity | Initial stock must not exceed the warehouse capacity. |
| Archive | Sets `archivedAt` timestamp; archived warehouses are excluded from all rule checks. |

### Replace Warehouse (additional rules)

| Rule | Detail |
|---|---|
| BUC must be active | The target BUC must exist and not already be archived. |
| New capacity ≥ old stock | The replacement cannot lose existing goods. |
| New stock = old stock | Goods are transferred 1-for-1; no creation or destruction. |

### Fulfilment Associations

| Constraint | Detail |
|---|---|
| Constraint 1 | A product can be supplied to one store by **at most 2 warehouses**. |
| Constraint 2 | A store can be served by **at most 3 warehouses** (across all products). |
| Constraint 3 | A warehouse can hold **at most 5 distinct product types**. |
| Duplicate guard | The exact (warehouse, store, product) triple must be unique (checked before DB write). |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/fulfilment/application/monolith/
│   │   ├── exception/
│   │   │   └── GlobalExceptionMapper.java        # Catches all exceptions → 400/404/409; never 500
│   │   ├── location/
│   │   │   └── LocationGateway.java              # Static location list; resolveByIdentifier()
│   │   ├── stores/
│   │   │   ├── StoreResource.java                # CRUD REST endpoints
│   │   │   ├── StoreEvent.java                   # CDI event payload
│   │   │   └── StoreEventObserver.java           # Fires LegacyStoreManagerGateway AFTER_SUCCESS
│   │   ├── products/
│   │   │   └── ProductResource.java              # CRUD REST endpoints
│   │   ├── warehouses/
│   │   │   ├── adapters/
│   │   │   │   ├── database/
│   │   │   │   │   ├── DbWarehouse.java           # JPA entity
│   │   │   │   │   └── WarehouseRepository.java  # PanacheRepository adapter
│   │   │   │   └── restapi/
│   │   │   │       └── WarehouseResourceImpl.java # OpenAPI-generated interface impl
│   │   │   └── domain/
│   │   │       ├── models/Warehouse.java          # Pure domain model (no JPA annotations)
│   │   │       ├── ports/                         # Interfaces: WarehouseStore, LocationResolver, …
│   │   │       └── usecases/
│   │   │           ├── CreateWarehouseUseCase.java
│   │   │           ├── ArchiveWarehouseUseCase.java
│   │   │           └── ReplaceWarehouseUseCase.java
│   │   └── fulfilment/
│   │       ├── FulfilmentAssociation.java         # JPA entity
│   │       ├── FulfilmentAssociationRepository.java
│   │       ├── AssociateFulfilmentUseCase.java    # Enforces all 3 constraints + duplicate guard
│   │       ├── FulfilmentResource.java            # GET + POST /fulfilment
│   │       └── FulfilmentRequest.java             # Request DTO
│   └── resources/
│       ├── application.properties
│       ├── import.sql                             # Seed data (ON CONFLICT DO NOTHING)
│       ├── openapi/warehouse-openapi.yaml         # Spec-first contract for Warehouse API
│       └── META-INF/resources/index.html         # Self-contained management dashboard UI
└── test/
    └── java/com/fulfilment/application/monolith/
        ├── location/LocationGatewayTest.java
        ├── warehouses/domain/usecases/
        │   ├── CreateWarehouseUseCaseTest.java    # 6 tests
        │   ├── ArchiveWarehouseUseCaseTest.java   # 1 test
        │   └── ReplaceWarehouseUseCaseTest.java   # 4 tests
        ├── warehouses/adapters/restapi/
        │   └── WarehouseEndpointIT.java           # Integration test (requires Docker)
        └── fulfilment/
            └── AssociateFulfilmentUseCaseTest.java # 6 tests (incl. duplicate-triple guard)
```

---

## Getting Started

### Prerequisites

- **JDK 17+** — set `JAVA_HOME` to your JDK 17 installation
- **Docker Desktop** — required for Dev Services (PostgreSQL container spun up automatically)
- No manual database setup needed in dev/test mode

```bash
# Verify Java version
java -version   # must be 17+

# If JAVA_HOME is not set correctly on macOS:
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.0.3.1.jdk/Contents/Home"
```

### Clone & Build

```bash
git clone <repo-url>
cd fcs-interview-code-assignment-main/java-assignment
./mvnw compile
```

> **IntelliJ users** — see [`INTELLIJ_SETUP.md`](INTELLIJ_SETUP.md) for IDE configuration, annotation processing, and generated-sources setup.

---

## Running the Application

### Dev mode (recommended — hot reload enabled)

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.0.3.1.jdk/Contents/Home" \
  ./mvnw quarkus:dev
```

Quarkus Dev Services automatically starts a PostgreSQL container.  
The database schema is created and seed data (`import.sql`) loaded on startup.

| URL | Description |
|---|---|
| http://localhost:8080 | Management dashboard UI |
| http://localhost:8080/q/dev | Quarkus Dev UI (dev mode only) |
| http://localhost:8080/q/openapi | OpenAPI spec |

### JVM mode (after `mvnw package`)

```bash
# Start a PostgreSQL instance manually:
docker run --rm -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test \
           -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:16

# Run the JAR:
java -jar ./target/quarkus-app/quarkus-run.jar
```

---

## API Reference

### Warehouses

| Method | Path | Description |
|---|---|---|
| `GET` | `/warehouse` | List all active (non-archived) warehouses |
| `POST` | `/warehouse` | Create a new warehouse (all 5 business rules enforced) |
| `GET` | `/warehouse/{id}` | Get warehouse by numeric id or BUC |
| `DELETE` | `/warehouse/{id}` | Archive warehouse by numeric id or BUC |
| `POST` | `/warehouse/{buc}/replacement` | Replace warehouse (archive old, create new) |

**Warehouse JSON schema:**

```json
{
  "id": "1",
  "businessUnitCode": "MWH.001",
  "location": "AMSTERDAM-001",
  "capacity": 100,
  "stock": 42
}
```

### Stores

| Method | Path | Description |
|---|---|---|
| `GET` | `/store` | List all stores |
| `POST` | `/store` | Create a store |
| `PUT` | `/store/{id}` | Update a store |
| `DELETE` | `/store/{id}` | Delete a store |

### Products

| Method | Path | Description |
|---|---|---|
| `GET` | `/product` | List all products |
| `POST` | `/product` | Create a product |
| `PUT` | `/product/{id}` | Update a product |
| `DELETE` | `/product/{id}` | Delete a product |

### Fulfilment Associations (Bonus)

| Method | Path | Description |
|---|---|---|
| `GET` | `/fulfilment` | List all associations |
| `POST` | `/fulfilment` | Create association (all 3 constraints + duplicate guard enforced) |

**Fulfilment request body:**

```json
{
  "warehouseId": 1,
  "storeId": 2,
  "productId": 3
}
```

### Error responses

All errors return a JSON body with a `message` field. HTTP status codes used:

| Code | Meaning |
|---|---|
| `400` | Business rule violation or bad input |
| `404` | Entity not found |
| `409` | Duplicate fulfilment association triple |

---

## Running Tests

### Unit tests only (no Docker required)

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.0.3.1.jdk/Contents/Home" \
  ./mvnw test -Dtest="!ProductEndpointTest,!WarehouseEndpointIT"
```

Expected output: **19 tests, 0 failures**.

### All tests (Docker required for integration tests)

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.0.3.1.jdk/Contents/Home" \
  ./mvnw test
```

### Test coverage summary

| Test class | Tests | What it covers |
|---|---|---|
| `LocationGatewayTest` | 2 | `resolveByIdentifier` — found and not-found |
| `CreateWarehouseUseCaseTest` | 6 | All 5 creation validations + happy path |
| `ArchiveWarehouseUseCaseTest` | 1 | Sets `archivedAt`, calls `update` |
| `ReplaceWarehouseUseCaseTest` | 4 | Not-found, capacity check, stock check, happy path |
| `AssociateFulfilmentUseCaseTest` | 6 | Duplicate guard + all 3 fulfilment constraints |

---

## UI Dashboard

The self-contained HTML dashboard at `src/main/resources/META-INF/resources/index.html` provides a tabbed management interface with no external CDN dependencies.

| Tab | Features |
|---|---|
| 📦 Products | CRUD (create, edit, delete); XSS-safe rendering |
| 🏪 Stores | CRUD; legacy system synced after DB commit via CDI events |
| 🏭 Warehouses | Create, archive, replace; utilisation badge (green/yellow/red) |
| 🔗 Fulfilment | Associate warehouse → store → product; reference ID tables |
| 📍 Locations | Static location grid with capacity limits |

All form labels and card headings include **`?` tooltip icons** describing the relevant business rule or constraint. Hover to reveal.

---

## Design Decisions

### Store: CDI events for post-commit legacy sync

`StoreResource` fires a CDI `StoreEvent` instead of calling `LegacyStoreManagerGateway` inline.  
`StoreEventObserver` observes with `@Observes(during = TransactionPhase.AFTER_SUCCESS)`, ensuring the legacy system is only notified once the database transaction has successfully committed.

### Global exception handling

`GlobalExceptionMapper` catches all exceptions and walks the cause chain:

- SQL state `23505` (unique violation) → **409 Conflict**
- `WebApplicationException` → passed through as-is
- `IllegalArgumentException` / anything else → **400 Bad Request**
- Never returns 500 to the client

### Fulfilment duplicate guard

Before reaching the constraint checks, `AssociateFulfilmentUseCase` calls `FulfilmentAssociationRepository.associationExists()` to detect exact duplicate triples. This avoids a raw database constraint error and returns a clean 400 with a descriptive message.

### Seed data idempotency

All `INSERT` statements in `import.sql` use `ON CONFLICT DO NOTHING`, making restarts and Quarkus Dev hot-reloads safe without duplicate key violations.
