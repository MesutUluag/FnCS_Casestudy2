# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

### 1. Understanding the Cost Structure

**Key Questions:**
- What specific cost categories need tracking? (Based on the domain: warehouse operations, store operations, inventory holding, inter-facility transfers)
- At what granularity should costs be captured? (Per warehouse, per store, per product, per location?)
- How do we handle archived warehouses with historical cost data when they're replaced?
- Should costs follow the Business Unit Code (survives warehouse replacement) or the physical warehouse instance?

### 2. Cost Attribution Challenges

**Warehouses:**
- **Direct costs**: Labor specific to warehouse operations, utilities, rent for that facility
- **Variable costs**: Handling costs per unit, storage costs based on capacity utilization
- **Shared costs**: Regional management overhead, centralized IT systems
- **Challenge**: When a warehouse like MWH.001 is replaced (archived), how do we allocate its historical costs vs the new warehouse under same Business Unit Code?

**Stores:**
- **Direct costs**: Store-specific labor, rent, local marketing
- **Allocation question**: If a store receives from multiple warehouses (max 3 warehouses per store), how to split inbound logistics costs?

**Products:**
- **Cost drivers**: Storage space consumption, handling frequency, special requirements
- **Challenge**: Products can be fulfilled from max 2 warehouses per store - how to attribute warehouse costs to product profitability?

### 3. Location-Based Cost Considerations

**Per the data model, Locations have constraints:**
- `maxNumberOfWarehouses`: Impacts economies of scale
- `maxCapacity`: Resource sharing implications
- Different locations (AMSTERDAM vs ZWOLLE) may have vastly different cost structures (labor rates, rent, regulations)

**Questions:**
- Should location-level overhead be allocated proportionally by warehouse capacity or actual stock levels?
- How to handle location costs when warehouse count varies (scale effects)?

### 4. Temporal Dimensions

**Warehouse Lifecycle:**
- `createdAt` and `archivedAt` timestamps exist for warehouses
- **Question**: How to handle cost allocation during transition periods (warehouse replacement)?
- **Consideration**: Should archived warehouses continue accumulating certain costs (e.g., lease obligations, decommissioning)?

**Cost Recognition:**
- When should transportation costs be allocated - at shipment, delivery, or period-end?
- How to handle accruals for costs not yet invoiced?

### 5. Allocation Methodologies

**Potential Drivers:**
- **Labor**: Hours worked per facility/product
- **Storage costs**: Based on `stock` levels and `capacity` utilization
- **Transportation**: Volume/weight shipped between warehouses and stores
- **Overhead**: Multiple bases - square footage, transaction volume, revenue

**Business Unit Code Consideration:**
- Since businessUnitCode survives warehouse replacement, costs should potentially accumulate at this level for P&L continuity
- This enables year-over-year comparison despite physical infrastructure changes

### 6. Data Integration Requirements

**Key Information Needed:**
- **Payroll system**: Labor hours and costs by location/facility
- **WMS (Warehouse Management System)**: Actual handling transactions, storage utilization
- **TMS (Transportation Management)**: Shipment costs, routes, frequencies
- **Accounting system**: Actual invoices, accruals, overhead allocations
- **Operational metrics**: Order volumes, product movements between entities

### 7. Reporting & Decision Support

**Stakeholder Needs:**
- **Operations**: Warehouse efficiency metrics (cost per unit handled, capacity utilization)
- **Finance**: P&L by Business Unit Code, location, product line
- **Strategy**: Make vs buy decisions (own warehouse vs 3PL), network optimization

**Key Decisions This Data Supports:**
- Should we replace underperforming warehouses? (Using historical cost data preserved through businessUnitCode)
- Which stores are most cost-effective to serve from which warehouses?
- What's the true profitability of each product considering full supply chain costs?

### 8. Practical Implementation Considerations

**Given the Constraint Model:**
- Max 2 warehouses can fulfill same product to a store → Allocate based on actual fulfillment volume
- Max 3 warehouses serve any store → Clearer cost allocation boundaries
- Max 5 products per warehouse → Limits complexity of product-level allocations

**Accuracy vs Effort Trade-off:**
- 80/20 rule: Focus precision on high-value products/facilities
- Use simpler allocation for overhead vs activity-based costing for direct variable costs
- Balance granularity with data collection burden

### 9. Historical Data & Continuity

**Critical for Warehouse Replacement Scenario:**
- When warehouse is archived (`archivedAt` populated), cost history must remain queryable
- New warehouse inherits businessUnitCode → Financial reporting continuity
- **Question**: Should budget/forecast remain constant for Business Unit Code, or reset for new physical facility?

### 10. Validation & Controls

**Reconciliation Requirements:**
- Total allocated costs = Total actual costs (within acceptable variance)
- Cross-entity allocations balance (warehouse charges to stores = store receipts from warehouses)
- Period-over-period variance analysis flags anomalies

**Audit Trail:**
- Document allocation rules and driver metrics
- Retain calculation logic even after warehouse replacement
- Enable drill-down from summary costs to source transactions

**Summary:** This approach bridges technical feasibility with business value by grounding cost allocation in the actual operational model (warehouses with capacity constraints, stores with fulfillment limits, locations with resource boundaries) while supporting strategic decisions like warehouse replacement and network optimization.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
[ fill here your answer ]

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
[ fill here your answer ]

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
[ fill here your answer ]

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
[ fill here your answer ]

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
