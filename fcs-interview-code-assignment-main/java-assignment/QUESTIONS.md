# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
The codebase has three different persistence strategies:

1. `Store` uses Panache's Active Record pattern — the entity itself has `persist()`, `findById()`,
   etc. directly on it. This is concise and great for simple CRUD, but it couples the domain model
   tightly to the ORM framework, making it harder to test business logic in isolation.

2. `Product` uses a separate `ProductRepository` (Panache Repository pattern) which is slightly
   cleaner, but still exposes Panache directly to the resource layer with no port/adapter
   separation.

3. `Warehouse` uses a full hexagonal (ports & adapters) design: a domain port `WarehouseStore` is
   defined as an interface, and `WarehouseRepository` is an infrastructure adapter that implements
   it. Use cases depend only on the port (interface), not on Panache — making them unit-testable
   with plain Mockito mocks, without spinning up a database.

If I were maintaining this codebase, I would standardise on the Warehouse approach for any entity
with non-trivial business rules. The key benefit is testability: use-case logic can be exercised
with fast, in-memory unit tests, and the JPA adapter is tested separately. For truly simple CRUD
entities with no business rules (e.g. reference data), the Panache Repository pattern is acceptable
and avoids unnecessary boilerplate.

I would not migrate everything at once — I'd apply the hexagonal pattern to entities as they grow
in complexity, and keep the active record approach only where the domain has no logic beyond basic
CRUD.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec-first (OpenAPI YAML → code generation, as used for Warehouse):

Pros:
- Single source of truth for the API contract — clients, servers, and documentation are always
  aligned.
- Enables client SDK generation, allowing consumers to generate typed clients automatically.
- The contract can be shared and agreed upon before any implementation begins, which works well
  in API-first or cross-team scenarios.
- Forces deliberate API design: you think about resources, schemas, and status codes upfront.
- Tooling (linters, validators) can catch contract drift in CI.

Cons:
- More ceremony to set up (generator plugin, YAML schema, generated code to understand).
- Generated code can be less idiomatic Java; changes to the YAML require a re-generation step.
- In a fast-moving team, YAML and code can still drift if discipline is not enforced.
- Overkill for small internal APIs that change frequently.

Code-first (JAX-RS annotations directly, as used for Product and Store):

Pros:
- Faster to iterate; no extra tooling or generation step.
- Code is idiomatic and readable — no abstraction layer between implementation and intent.
- Well-suited for internal services where consumers are in the same codebase.

Cons:
- No machine-readable contract by default (requires Quarkus SmallRye OpenAPI to scan annotations
  retrospectively, which can be incomplete or inaccurate).
- Harder to coordinate changes with external consumers.
- API shape can evolve inconsistently across developers.

My choice:
I prefer spec-first for any API that is consumed by external teams, published to a gateway, or
expected to be stable. Code-first is acceptable for internal, rapidly-evolving endpoints within
a single team. In this codebase, given it exposes a warehouse management API that's likely consumed
by other systems, I would adopt spec-first (as done for Warehouse) as the standard and backfill
YAML specs for Store and Product when bandwidth allows.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority 1 — Use-Case Unit Tests (highest ROI):
The use-case classes (CreateWarehouseUseCase, ReplaceWarehouseUseCase, etc.) contain all business
rules and validations. These are pure Java, have no framework dependencies, and can be tested
exhaustively with Mockito mocks for the port interfaces. They run in milliseconds and cover the
most critical correctness constraints — missing a validation is a business bug. I'd aim to cover
all validation branches (happy path + each failure case) for every use case.

Priority 2 — Integration Tests for REST Adapters (@QuarkusTest):
A handful of integration tests that exercise the full stack (HTTP → resource → use case → DB) give
confidence that the wiring is correct. These are slower (require a database) but validate that
Quarkus injection, transactions, and HTTP response codes work end-to-end. Focus on the happy path
and at least one error path per endpoint.

Priority 3 — Repository/Adapter Tests:
Database adapter methods (WarehouseRepository) benefit from a @QuarkusTest that actually hits an
H2 or test PostgreSQL database. These are lower priority than use-case tests but ensure the ORM
queries are correct.

What I skip or defer:
I would not write tests for trivial getter/setter code, generated code, or infrastructure
configuration.

Keeping coverage effective over time:
1. Enforce a CI gate — fail the build if coverage drops below a target threshold (e.g. 80% on
   domain classes).
2. Require a test for every bug fix — the test should reproduce the bug before the fix is applied.
3. Treat flaky tests as priority bugs — a test that sometimes passes destroys confidence faster
   than no test at all.
4. Review tests in code review with the same rigour as production code.
```
