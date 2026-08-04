# FnCS Interview Case Study Summary

## Overview
This is a **Fulfillment & Cost Study (FnCS)** interview case study with two main components:

---

## 1. Case Study Discussion (case-study/)

5 scenarios focused on cost control in warehouse/fulfillment operations:

### Scenario 1: Cost Allocation and Tracking
Track and allocate costs across warehouses/stores (labor, inventory, transportation, overhead)

### Scenario 2: Cost Optimization Strategies
Identify and implement cost optimization strategies without compromising service quality

### Scenario 3: Integration with Financial Systems
Integrate Cost Control Tool with financial systems for real-time data synchronization

### Scenario 4: Budgeting and Forecasting
Develop budgeting and forecasting capabilities for fulfillment operations

### Scenario 5: Cost Control in Warehouse Replacement
Preserve cost history when replacing warehouses while keeping operations within budget

**Requirements:** Provide thoughtful, business-oriented answers bridging technical and business perspectives in CASE_STUDY.md

---

## 2. Java Code Assignment (java-assignment/)

### Tech Stack
- **Framework:** Quarkus 3.13.3
- **Java:** JDK 17+
- **Database:** PostgreSQL
- **Persistence:** Hibernate ORM with Panache
- **API:** REST/JAX-RS, OpenAPI Generator (for Warehouse API)

### Domain Model

**Entities:**
- `Location` - geographical place/city
- `Store` - physical stores where products are sold
- `Warehouse` - distribution centers supplying stores
- `Product` - goods sold to customers

**Key Feature:** Warehouse "replace" operation - archives old warehouse and creates new one with same Business Unit Code for history tracking

### Required Tasks

#### Task 1: Location (Must Have) - WARM-UP
**Package:** `com.fulfilment.application.monolith.location`

Implement `LocationGateway.resolveByIdentifier()` method

#### Task 2: Store (Must Have)
**Package:** `com.fulfilment.application.monolith.stores`

Adjust `StoreResource` to ensure `LegacyStoreManagerGateway` calls happen **AFTER** database commits to guarantee downstream legacy system receives confirmed data

#### Task 3: Warehouse (Must Have)
**Package:** `com.fulfilment.application.monolith.warehouse`

Implement API endpoints and use cases for warehouse operations (create, retrieve, replace, archive) with validations:

**Business Validations:**
- Business Unit Code uniqueness check
- Location must exist and be valid
- Check warehouse creation limits per location
- Capacity validation (not exceeding location max, can handle stock)

**Additional Validations for Replace Operation:**
- New warehouse capacity must accommodate replaced warehouse stock
- Stock must match between old and new warehouse

#### Bonus Task (Nice to Have)
Implement warehouse-to-store-product fulfillment associations with constraints:
- Each Product can be fulfilled by max 2 different Warehouses per Store
- Each Store can be fulfilled by max 3 different Warehouses
- Each Warehouse can store max 5 types of Products

### Questions to Answer (QUESTIONS.md)

1. Database access layer implementation strategy preferences and refactoring approach
2. OpenAPI spec generation vs direct coding - pros/cons and preference
3. Test prioritization strategy and maintaining effective coverage over time

---

## Project Structure

```
fcs-interview-code-assignment-main/
├── case-study/
│   ├── BRIEFING.md          # Domain briefing
│   └── CASE_STUDY.md        # 5 scenarios to discuss
├── java-assignment/
│   ├── CODE_ASSIGNMENT.md   # Implementation tasks
│   ├── QUESTIONS.md         # Questions to answer
│   ├── README.md            # Setup instructions
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fulfilment/application/monolith/
│       │   ├── location/
│       │   ├── stores/
│       │   ├── warehouses/
│       │   └── products/
│       └── test/
```

---

## Running the Application

### Prerequisites
- JDK 17+
- PostgreSQL (or Docker)

### Development Mode (with hot reload)
```bash
cd java-assignment
./mvnw quarkus:dev
```

### Run PostgreSQL with Docker
```bash
docker run -it --rm=true --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

### Access Application
http://localhost:8080/index.html

---

## Expected Effort

~4 hours for senior developers with AI assistance 🤖

---

## Files to Complete

- [x] case-study/CASE_STUDY.md - Answer 5 scenarios
- [x] java-assignment/QUESTIONS.md - Answer 3 questions
- [x] LocationGateway.resolveByIdentifier() implementation
- [x] StoreResource transactional fixes (CDI event + AFTER_SUCCESS observer)
- [x] Warehouse CRUD operations with validations
- [x] Warehouse replace operation
- [x] Tests for implementations (22 unit tests + integration test)
- [x] (Bonus) Warehouse-Store-Product associations with 422 constraint error responses
