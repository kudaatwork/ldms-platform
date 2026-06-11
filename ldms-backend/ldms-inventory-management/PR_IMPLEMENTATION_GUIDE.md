# Purchase Requisition (PR) Implementation Guide

## Overview
This document provides a comprehensive guide to the Purchase Requisition feature implementation in the LDMS Inventory Management Service.

---

## 1. Data Model

### Core Entities Created

#### 1.1 PurchaseRequisition (Header)
**Location**: `model/PurchaseRequisition.java`

**Key Features**:
- Internal procurement request (does NOT financially commit organization)
- Must be approved before procurement action
- Tracks full lifecycle from Draft to Fulfilled/Closed
- Supports versioning via amendments
- Maintains complete audit trail

**Status Lifecycle**:
```
DRAFT → SUBMITTED → APPROVED → PARTIALLY_FULFILLED → FULFILLED
         ↓              ↓
      REJECTED      CANCELLED/EXPIRED/CLOSED
```

**Critical Fields**:
- `requisitionNumber`: Unique identifier
- `status`: Current workflow status
- `priority`: LOW, NORMAL, HIGH, URGENT
- `requiredByDate`: When items are needed
- `expiryDate`: Auto-expire if not fulfilled
- `estimatedTotal`: For approval threshold checks
- `defaultFulfillmentMethod`: PURCHASE, FROM_STOCK, TRANSFER, DEFERRED

**Workflow Tracking**:
- Submission: `submittedAt`, `submittedByUserId`
- Approval: `approvedAt`, `approvedByUserId`, `approvalNotes`
- Rejection: `rejectedAt`, `rejectedByUserId`, `rejectionReason`
- Cancellation: `cancelledAt`, `cancelledByUserId`, `cancellationReason`

---

#### 1.2 PurchaseRequisitionLine (Line Items)
**Location**: `model/PurchaseRequisitionLine.java`

**Key Features**:
- Line-level fulfillment tracking
- Multiple fulfillment methods per requisition
- Supports partial fulfillment from different sources

**CRITICAL Quantity Tracking** (Core Business Logic):
```
requested_quantity          : Original request amount
approved_quantity          : What was approved (may differ from requested)
ordered_quantity           : Amount ordered via PO (auto-updated when PO created)
fulfilled_from_stock_quantity : Fulfilled from existing inventory
fulfilled_from_transfer_quantity : Fulfilled via internal warehouse transfer
remaining_quantity         : Calculated = approved - (ordered + stock + transfer)
```

**Fulfillment Methods**:
- `PURCHASE`: Create PO from supplier
- `FROM_STOCK`: Fulfill from existing inventory
- `TRANSFER`: Internal stock transfer between warehouses
- `DEFERRED`: Procure later
- `NOT_REQUIRED`: No longer needed

**Business Methods**:
- `getTotalFulfilledQuantity()`: Sum of all fulfillment sources
- `calculateRemainingQuantity()`: Auto-calculate what's left
- `isFullyFulfilled()`: Check if line is complete
- `recordPurchaseOrderFulfillment(qty)`: Update when PO created
- `recordStockFulfillment(qty)`: Update when fulfilled from stock
- `recordTransferFulfillment(qty)`: Update when fulfilled via transfer

---

#### 1.3 PurchaseRequisitionAmendment (Audit Trail)
**Location**: `model/PurchaseRequisitionAmendment.java`

**Purpose**: Track changes to APPROVED PRs (which cannot be edited directly)

**Amendment Types**:
- `QUANTITY_CHANGE`: Approved qty adjusted
- `FULFILLMENT_METHOD_CHANGE`: Changed how item will be fulfilled
- `CLOSURE`: Administrative closure with reason
- `LINE_ADDITION`: New line added to approved PR
- `LINE_REMOVAL`: Line removed from approved PR

**Fields**:
- `amendmentNumber`: Sequential number per PR
- `description`: What changed
- `previousValue`, `newValue`: Before/after (JSON or text)
- `reason`: Business justification
- `lineId`: If amendment affects specific line

---

#### 1.4 Integration with Purchase Order
**Modified Files**:
- `PurchaseOrder.java`: Added `purchaseRequisitionId` field
- `PurchaseOrderLine.java`: Added `purchaseRequisitionLineId` field

**Purpose**: Track which POs were created from which PRs for audit trail

---

## 2. Database Schema

### Migration: V3__create_sales_orders_tables.sql

**Tables Created**:
1. `purchase_requisition` - Header table
2. `purchase_requisition_line` - Line items
3. `purchase_requisition_amendment` - Change history

**Indexes Added**:
- Unique index on `requisition_number`
- Composite indexes on `department_id + status`
- Composite indexes on `requested_by_user_id + status`
- Date-based indexes for queries
- Fulfillment method index for filtering

**Foreign Keys**:
- PR Line → PR (CASCADE delete)
- Amendment → PR (CASCADE delete)

**Alterations to Existing Tables**:
- `purchase_order.purchase_requisition_id` - Links PO to source PR
- `purchase_order_line.purchase_requisition_line_id` - Links PO line to PR line

---

## 3. Repository Layer

### Created Repositories

#### 3.1 PurchaseRequisitionRepository
**Location**: `repository/PurchaseRequisitionRepository.java`

**Custom Queries**:
- `findPendingApprovalsByDepartment()`: All SUBMITTED PRs for dept
- `findApprovedPendingFulfillment()`: Approved PRs ready for fulfillment
- `findExpiringSoon()`: PRs expiring in next N days
- `findExpired()`: PRs past expiry date
- `countByStatusForOrganization()`: Dashboard metrics

#### 3.2 PurchaseRequisitionLineRepository
**Location**: `repository/PurchaseRequisitionLineRepository.java`

**Custom Queries**:
- `findLinesWithRemainingQuantity()`: Lines needing fulfillment
- `findLinesEligibleForPurchaseOrder()`: Lines with `fulfillmentMethod=PURCHASE` and `remainingQty > 0`
- `findByFulfillmentMethod()`: Filter by how item will be fulfilled

#### 3.3 PurchaseRequisitionAmendmentRepository
**Location**: `repository/PurchaseRequisitionAmendmentRepository.java`

**Features**:
- Track all changes to PRs
- Auto-increment amendment numbers
- Full audit history

---

## 4. Specifications (Type-Safe Querying)

### PurchaseRequisitionSpecification
**Location**: `repository/specification/PurchaseRequisitionSpecification.java`

**Available Filters**:
- By organization, department, requester
- By status, priority
- By date ranges (requisition, required, expiry, approval)
- By estimated total (range queries)
- By cost center, project code
- By preferred supplier, target warehouse
- Full-text search across multiple fields

**Usage Example**:
```java
Specification<PurchaseRequisition> spec = Specification
    .where(PurchaseRequisitionSpecification.organizationIdEquals(orgId))
    .and(PurchaseRequisitionSpecification.statusEquals(APPROVED))
    .and(PurchaseRequisitionSpecification.requiredByDateBefore(LocalDate.now().plusDays(7)))
    .and(PurchaseRequisitionSpecification.deleted());

List<PurchaseRequisition> urgentPRs = prRepository.findAll(spec);
```

---

## 5. Business Logic (To Be Implemented)

### Core Services Needed

#### 5.1 PurchaseRequisitionService (API Interface)
**Location**: `business/logic/api/PurchaseRequisitionService.java`

**Methods**:
```java
// CRUD Operations
PurchaseRequisition createDraft(CreatePRRequest request);
PurchaseRequisition update(Long id, UpdatePRRequest request);
PurchaseRequisition findById(Long id);
Page<PurchaseRequisition> search(PRSearchCriteria criteria, Pageable pageable);

// Workflow Operations
PurchaseRequisition submit(Long id);
PurchaseRequisition approve(Long id, ApprovalRequest request);
PurchaseRequisition reject(Long id, RejectionRequest request);
PurchaseRequisition cancel(Long id, CancellationRequest request);

// Fulfillment Operations
void markLineAsFullyFulfilled(Long prId, Long lineId);
void recordStockFulfillment(Long lineId, BigDecimal quantity);
void recordTransferFulfillment(Long lineId, BigDecimal quantity);

// Amendment Operations
PurchaseRequisitionAmendment createAmendment(Long prId, AmendmentRequest request);

// Utility
void checkAndExpirePRs(); // Scheduled job
```

#### 5.2 PurchaseRequisitionFulfillmentService
**Purpose**: Handle fulfillment logic

**Methods**:
```java
// Determine if line can be fulfilled from stock
boolean canFulfillFromStock(Long prLineId);

// Fulfill from existing inventory
void fulfillFromStock(Long prLineId, FulfillFromStockRequest request);

// Create stock transfer for fulfillment
InventoryTransfer createTransferForFulfillment(Long prLineId, TransferRequest request);

// Check all fulfillment options
FulfillmentOptions analyzeFulfillmentOptions(Long prLineId);
```

#### 5.3 PurchaseToPOConversionService
**Purpose**: Create POs from approved PRs

**Key Method**:
```java
PurchaseOrder createPurchaseOrderFromPR(Long prId, CreatePOFromPRRequest request);
```

**Logic**:
1. Validate PR is APPROVED
2. Get lines with `fulfillmentMethod = PURCHASE` and `remainingQuantity > 0`
3. Create PO in DRAFT status
4. Link PO to PR via `purchaseRequisitionId`
5. Create PO lines linked to PR lines via `purchaseRequisitionLineId`
6. Update PR line `orderedQuantity`
7. Recalculate `remainingQuantity`
8. If all lines fulfilled, update PR status to FULFILLED

**Important**:
- PO starts in DRAFT (procurement team can adjust)
- Multiple POs can be created from one PR (splitting)
- Multiple PRs can be consolidated into one PO

---

## 6. State Transition Rules

### PR Header Status Transitions

```
DRAFT:
  → SUBMITTED (if has lines)
  → CANCELLED (at any time)

SUBMITTED:
  → APPROVED (approval granted)
  → REJECTED (approval denied)
  → CANCELLED (requester cancels)

APPROVED:
  → PARTIALLY_FULFILLED (some lines fulfilled)
  → FULFILLED (all lines fulfilled)
  → CLOSED (admin closure)
  → CANCELLED (with reason)
  → EXPIRED (past expiry date)

PARTIALLY_FULFILLED:
  → FULFILLED (all lines completed)
  → CLOSED (admin closure of remaining)
  → CANCELLED (with reason)

FULFILLED:
  → CLOSED (final closure)

REJECTED/CANCELLED/EXPIRED/CLOSED:
  → (Terminal states - no further transitions)
```

### Validation Rules

**On Submit**:
- Must have at least one line
- All lines must have requested quantity > 0
- Purpose must be provided

**On Approve**:
- Must be in SUBMITTED status
- Can set approved_quantity != requested_quantity per line
- Must provide approval notes if quantities adjusted

**On Fulfillment**:
- Only APPROVED or PARTIALLY_FULFILLED PRs can be fulfilled
- Cannot fulfill more than approved_quantity
- Must specify fulfillment method per line

**On PO Creation**:
- PR must be APPROVED
- Only create PO for lines with fulfillmentMethod = PURCHASE
- Only include lines with remainingQuantity > 0

---

## 7. DTOs and Request Objects (To Be Created)

### Request DTOs

#### CreatePurchaseRequisitionRequest
```java
{
  "purpose": "Q1 2025 office supplies replenishment",
  "justification": "Current stock below reorder point",
  "priority": "NORMAL",
  "requiredByDate": "2025-02-15",
  "departmentId": 123,
  "targetWarehouseId": 45,
  "lines": [
    {
      "productId": 789,
      "requestedQuantity": 100,
      "unitOfMeasure": "UNITS",
      "estimatedUnitPrice": 25.50,
      "fulfillmentMethod": "PURCHASE",
      "specifications": "Black ink, HP compatible"
    }
  ]
}
```

#### ApprovePurchaseRequisitionRequest
```java
{
  "approvalNotes": "Approved with quantity adjustments",
  "lineAdjustments": [
    {
      "lineId": 1,
      "approvedQuantity": 80,  // Reduced from 100
      "adjustmentReason": "Budget constraints"
    }
  ]
}
```

#### CreatePOFromPRRequest
```java
{
  "purchaseRequisitionId": 123,
  "supplierId": 456,
  "expectedDate": "2025-02-01",
  "lineSelections": [
    {
      "prLineId": 1,
      "quantityToOrder": 50  // Partial fulfillment
    },
    {
      "prLineId": 2,
      "quantityToOrder": 100  // Full fulfillment
    }
  ]
}
```

### Response DTOs

#### PurchaseRequisitionDTO
```java
{
  "id": 123,
  "requisitionNumber": "PR-2025-00001",
  "status": "APPROVED",
  "priority": "HIGH",
  "requestedBy": {...},  // User details from user service
  "department": {...},   // Dept details
  "estimatedTotal": 5250.00,
  "requiredByDate": "2025-02-15",
  "lines": [...],
  "fulfillmentSummary": {
    "totalLines": 5,
    "fullyFulfilledLines": 2,
    "partiallyFulfilledLines": 1,
    "pendingLines": 2
  }
}
```

---

## 8. REST API Endpoints (To Be Created)

### Frontend Endpoints

```
POST   /api/v1/frontend/purchase-requisitions              Create draft PR
GET    /api/v1/frontend/purchase-requisitions/{id}        Get PR details
PUT    /api/v1/frontend/purchase-requisitions/{id}        Update draft PR
DELETE /api/v1/frontend/purchase-requisitions/{id}        Soft delete PR

POST   /api/v1/frontend/purchase-requisitions/{id}/submit   Submit for approval
POST   /api/v1/frontend/purchase-requisitions/{id}/cancel   Cancel PR

GET    /api/v1/frontend/purchase-requisitions/my-requests   My PRs
GET    /api/v1/frontend/purchase-requisitions/pending-approval   PRs I can approve
GET    /api/v1/frontend/purchase-requisitions/search        Advanced search
```

### System Endpoints (Internal/Admin)

```
POST   /api/v1/system/purchase-requisitions/{id}/approve    Approve PR
POST   /api/v1/system/purchase-requisitions/{id}/reject     Reject PR

POST   /api/v1/system/purchase-requisitions/{id}/fulfill-line    Record fulfillment
POST   /api/v1/system/purchase-requisitions/{id}/create-po       Create PO from PR
POST   /api/v1/system/purchase-requisitions/{id}/amend           Create amendment

GET    /api/v1/system/purchase-requisitions/expiring-soon   PRs expiring soon
POST   /api/v1/system/purchase-requisitions/expire-overdue  Expire old PRs (scheduled)
GET    /api/v1/system/purchase-requisitions/dashboard       Metrics by status
```

---

## 9. Security Roles (To Be Created)

### Location: `utils/security/PurchaseRequisitionRoles.java`

```java
public class PurchaseRequisitionRoles {
    // Create and manage own PRs
    public static final String CREATE_PR = "ROLE_CREATE_PURCHASE_REQUISITION";
    public static final String VIEW_OWN_PR = "ROLE_VIEW_OWN_PURCHASE_REQUISITION";
    public static final String UPDATE_OWN_PR = "ROLE_UPDATE_OWN_PURCHASE_REQUISITION";
    public static final String CANCEL_OWN_PR = "ROLE_CANCEL_OWN_PURCHASE_REQUISITION";

    // Department head / approver roles
    public static final String APPROVE_PR = "ROLE_APPROVE_PURCHASE_REQUISITION";
    public static final String REJECT_PR = "ROLE_REJECT_PURCHASE_REQUISITION";
    public static final String VIEW_DEPT_PR = "ROLE_VIEW_DEPARTMENT_PURCHASE_REQUISITION";

    // Procurement team roles
    public static final String FULFILL_PR = "ROLE_FULFILL_PURCHASE_REQUISITION";
    public static final String CREATE_PO_FROM_PR = "ROLE_CREATE_PO_FROM_PURCHASE_REQUISITION";
    public static final String AMEND_PR = "ROLE_AMEND_PURCHASE_REQUISITION";

    // Admin roles
    public static final String VIEW_ALL_PR = "ROLE_VIEW_ALL_PURCHASE_REQUISITION";
    public static final String CLOSE_PR = "ROLE_CLOSE_PURCHASE_REQUISITION";
}
```

---

## 10. Integration Points

### With Existing Systems

#### 10.1 User Management Service
- Fetch user details for requester, approver
- Validate user has permission for department
- Get department hierarchy for approval routing

#### 10.2 Organization Service
- Validate department exists
- Get organization defaults
- Fetch cost centers and project codes

#### 10.3 Inventory Service
- Check stock availability for FROM_STOCK fulfillment
- Reserve stock when fulfilling from inventory
- Create stock adjustments when fulfilled

#### 10.4 Purchase Order Service
- Create PO from approved PR
- Link PO lines to PR lines
- Update PR line quantities when PO created

#### 10.5 Notifications Service
- Notify approvers when PR submitted
- Notify requester on approval/rejection
- Alert on PRs expiring soon

---

## 11. Scheduled Jobs

### Expiry Check Job
```java
@Scheduled(cron = "0 0 1 * * *")  // Daily at 1 AM
public void expireOverduePRs() {
    LocalDate today = LocalDate.now();
    List<PurchaseRequisition> expired = prRepository.findExpired(today, EntityStatus.DELETED);

    for (PurchaseRequisition pr : expired) {
        pr.setStatus(PurchaseRequisitionStatus.EXPIRED);
        prRepository.save(pr);
        // Send notification
    }
}
```

### Expiring Soon Alert Job
```java
@Scheduled(cron = "0 0 8 * * MON")  // Mondays at 8 AM
public void alertExpiringSoon() {
    LocalDate today = LocalDate.now();
    LocalDate oneWeekAhead = today.plusDays(7);

    List<PurchaseRequisition> expiring = prRepository.findExpiringSoon(
        today, oneWeekAhead, EntityStatus.DELETED
    );

    // Send alerts to requesters and procurement team
}
```

---

## 12. Testing Strategy

### Unit Tests Required

#### 12.1 Entity Tests
- `PurchaseRequisitionTest`: Test business methods, state transitions
- `PurchaseRequisitionLineTest`: Test quantity calculations, fulfillment tracking

#### 12.2 Repository Tests
- Test custom queries
- Test specifications
- Test pessimistic locking

#### 12.3 Service Tests
- Test PR creation, submission, approval workflow
- Test fulfillment logic
- Test PO creation from PR
- Test amendment creation

#### 12.4 Integration Tests
- Full workflow: Create → Submit → Approve → Fulfill
- Multiple fulfillment methods on same PR
- PO creation from PR with quantity updates
- Expiry handling

---

## 13. Key Design Decisions & Rationale

### Why This Architecture?

1. **Separation of PR and PO**:
   - PR = internal request (no financial commitment)
   - PO = external commitment (legally binding)
   - Allows approval before commitment

2. **Line-Level Fulfillment Tracking**:
   - Real-world scenarios: partial fulfillment common
   - Mixed fulfillment (some from stock, some via PO)
   - Accurate remaining quantity calculation

3. **Amendment System**:
   - Approved PRs are "locked"
   - Changes tracked via amendments
   - Full audit trail for compliance

4. **Fulfillment Method Enum**:
   - Prevents "PO created for everything" anti-pattern
   - Enables stock-first strategy
   - Supports internal transfers

5. **Soft References** (IDs, not foreign keys) to PO:
   - Allows PO deletion without cascading to PR
   - PR remains as historical record
   - PO can reference multiple PRs (consolidation)

---

## 14. Migration Path

### Phase 1: Core PR Workflow (Current Implementation)
- ✅ Database schema
- ✅ Entities
- ✅ Repositories
- ⏳ Service layer
- ⏳ REST APIs
- ⏳ Security

### Phase 2: Fulfillment Logic
- Stock availability checking
- Auto-PO creation
- Transfer creation
- Quantity reconciliation

### Phase 3: Advanced Features
- Multi-level approval routing
- Budget integration
- Supplier suggestions based on history
- PR templates
- Bulk operations

---

## 15. Files Created

### Entities
- `model/PurchaseRequisition.java`
- `model/PurchaseRequisitionLine.java`
- `model/PurchaseRequisitionAmendment.java`
- `model/PurchaseRequisitionStatus.java`
- `model/FulfillmentMethod.java`
- `model/PriorityLevel.java`

### Repositories
- `repository/PurchaseRequisitionRepository.java`
- `repository/PurchaseRequisitionLineRepository.java`
- `repository/PurchaseRequisitionAmendmentRepository.java`

### Specifications
- `repository/specification/PurchaseRequisitionSpecification.java`

### Metamodels
- `model/PurchaseRequisition_.java`
- `model/PurchaseRequisitionLine_.java`

### Database
- `db/migration/V3__create_sales_orders_tables.sql`

### Documentation
- `PR_IMPLEMENTATION_GUIDE.md` (this file)

---

## 16. Next Steps

1. **Complete Service Layer**:
   - Implement `PurchaseRequisitionService`
   - Implement `PurchaseRequisitionFulfillmentService`
   - Implement `PurchaseToPOConversionService`

2. **Create DTOs**:
   - Request objects
   - Response objects
   - Mapper classes

3. **Build REST Controllers**:
   - Frontend resource
   - System resource

4. **Add Security**:
   - Role definitions
   - Method-level security annotations

5. **Write Tests**:
   - Unit tests for all components
   - Integration tests for workflows

6. **Add RabbitMQ Events**:
   - PR_SUBMITTED
   - PR_APPROVED
   - PR_FULFILLED
   - PO_CREATED_FROM_PR

---

## Contact & Questions

For questions about this implementation, refer to:
- LDMS Project Documentation
- Spring Boot 3.4 Documentation
- Project LX Backend Patterns

---

**Document Version**: 1.0
**Last Updated**: 2026-01-03
**Author**: Claude Code Assistant
