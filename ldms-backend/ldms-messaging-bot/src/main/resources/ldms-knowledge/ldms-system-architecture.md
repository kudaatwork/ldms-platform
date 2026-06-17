# LDMS / Project LX — System Architecture

> Canonical reference for how the platform fits together. Details may evolve as implementation progresses.

## What is LDMS?

The **Logistics and Distribution Management System (LDMS)** is a digital platform that helps suppliers, transporters, clearing agents, and customers manage the entire road-based delivery process of goods (gas, fuel, cargo) across locations and borders.

**Goals:** efficiency, transparency, accountability across ordering, transport, tracking, and delivery.

### Who uses LDMS?

| Actor | Role |
|-------|------|
| **Suppliers** | Sell goods; manage customers, products, POs, fleet compliance |
| **Customers** | Place POs, receive goods, create GRVs |
| **Drivers** | Transport goods; GPS, stops, fuel/expense requests |
| **Transport companies** | Own trucks and drivers |
| **Clearing agents** | Border/legal clearance |
| **Service providers** | Fuel stations, mechanics, roadside support |
| **Admin** | Verify documents, approve orgs/users, platform monitoring |

---

## Microservice Blueprint (23 services)

### Infrastructure & platform

| # | Service | Role |
|---|---------|------|
| 1 | **API Gateway** | Single front door; routing, rate limiting, security, optional BFF aggregation |
| 2 | **Config Server** | Centralized technical config (DB URLs, API keys, feature flags) |
| 3 | **Eureka Server** | Service discovery — dynamic location of microservices |
| 20 | **Integration Service** | Adapters for 3rd-party APIs (telematics in, payment gateways out) |
| 21 | **Reporting Analytics Feeder** | Subscribes to events → transforms → loads analytics warehouse |
| 22 | **Audit Trail & Log Service** | Immutable log of all RabbitMQ events |
| 23 | **Scheduler/Job Service** | Time-based triggers (payment reminders, SLA alerts, compliance expiry) |

### Core identity & organization

| # | Service | Role |
|---|---------|------|
| 4 | **User Management** | Profiles, roles, groups — *who is this user and what can they do?* |
| 5 | **Authentication Management** | Login, JWTs, password reset, email verification — *is this user who they claim?* |
| 6 | **Organization Management** | Org profiles (supplier/customer/transporter/agent), branches, verification status |

### Core business & order flow

| # | Service | Role |
|---|---------|------|
| 9 | **Inventory Management** | ERP core: products, warehouses, stock, POs, sales orders, reservations, transfers, GRVs |
| 10 | **Shipment Management** | Dispatch after PO approval; documents; status → Ready to Release |
| 16 | **Billing & Payments** | Invoices (from GRVs), payment terms, payments, expense reconciliation |

### Logistics & fleet

| # | Service | Role |
|---|---------|------|
| 12 | **Fleet Management** | Trucks, trailers, drivers; compliance via Documents service |
| 13 | **Trip & Tracking** | Trip lifecycle, GPS/telemetry, trip events, live tracking |
| 14 | **Roadside & Support** | Mechanics, service stations; log roadside stops |
| 15 | **Fuel & Expenses** | Driver fuel/funds requests; in-trip operational costs |

### Supporting & utility

| # | Service | Role |
|---|---------|------|
| 7 | **Location Management** | Master address book — addresses, GPS, geofences (`location_id` refs) |
| 8 | **Reference/Config Management** | Business config lists (payment terms, document types, stop reasons) |
| 11 | **Documents Management** | Document metadata, versioning, expiry, entity links; file URL (S3/storage utility) |

### Integration & communication

| # | Service | Role |
|---|---------|------|
| 17 | **Notifications Management** | Outbound email, SMS, WhatsApp, in-app (template-driven from events) |
| 18 | **Messaging Inbound & Bot** | Inbound webhooks (e.g. WhatsApp); parse commands → RabbitMQ events |

### Admin & monitoring

| # | Service | Role |
|---|---------|------|
| 19 | **Platform Admin & Monitoring** | Cross-tenant read model for Consolidated Admin Portal (event-driven) |

**Note:** File upload is a low-level utility (often cloud infra or Documents service internal), not a standalone business service.

---

## End-to-end flow (developer guide)

### Phase A — Onboarding & identity

1. Supplier/Transporter registers org on Web Portal.
2. Gateway → **Organization Management** (org `Pending`) + **User Management** (first admin).
3. `user.created` → **Notifications** → **Authentication** (verification token) → verification email.
4. User clicks link → **Authentication** verifies and activates.
5. Ops approves org via Admin Portal → **Organization Management** sets Verified → `org.verified`.
6. **Notifications** sends confirmation.

**Services:** Organization, User, Authentication, Notifications, Platform Admin.

### Phase B — Master data & setup

1. Verified Supplier Admin logs in (**Authentication**).
2. **Inventory Management**: warehouses (via **Location**), products, categories, stock.
3. Product docs: Inventory → **Documents Management** (upload) → `document_id` stored on product.
4. Supplier creates Customer orgs via **Organization Management**.

**Services:** Inventory, Documents, Location, Organization.

### Phase C — Transaction initiation (PO & stock)

1. Customer creates PO → **Inventory Management**.
2. Inventory: payment terms (**Reference/Config** or Billing), validate, **reserve stock**, save PO → `po.created`.
3. Supplier approves → `po.approved`.
4. **Scheduler/Job**: hourly SLA check → `po.approval_sla_breached` → **Notifications**.

**Services:** Inventory, Reference/Config, Scheduler, RabbitMQ.

### Phase D — Dispatch & document control

1. Supplier creates Sales Order / Dispatch (**Inventory**) → **Shipment Management** creates Shipment.
2. Customs/legal docs: Shipment → **Documents Management**.
3. If advance payment: Shipment checks **Billing & Payments** before release.
4. Ready → `shipment.ready_for_pickup`.

**Services:** Inventory, Shipment, Documents, Billing.

### Phase E — Fleet & trip management

1. **Trip & Tracking** (or Fleet) listens for `shipment.ready_for_pickup`.
2. Fleet manager assigns truck + driver → Trip created.
3. **Fleet Management** compliance check via **Documents** (truck + driver docs valid).
4. Trip created → `trip.started`.
5. Location: Mobile app or **Integration** (telematics) → **Trip & Tracking**.
6. Stops logged via app or WhatsApp: **Messaging Inbound** parses → `trip.stop_recorded` → Trip service.

**Services:** Fleet, Trip & Tracking, Documents, Integration, Messaging Inbound.

### Phase F — In-trip support & expenses

1. Low fuel / fund request → **Fuel & Expenses** → `fund_request.created`.
2. Finance approves → **Fuel & Expenses** → **Billing** (expense reconciliation).
3. Mechanic stops: Trip → **Roadside & Support**.

**Services:** Fuel & Expenses, Billing, Trip & Tracking, Roadside & Support.

### Phase G — Delivery, receiving & returns

1. Driver marks Arrived → **Trip & Tracking**.
2. Customer receives against PO (**Inventory**): GRV number, stock increase → `grv.created`.
3. Exception: Purchase Return in **Inventory**.
4. Trip listens for `grv.created` → Delivered, return leg.

**Services:** Inventory (GRV/returns), Trip & Tracking.

### Phase H — Invoicing & payment

1. **Billing** listens for `grv.created` → Invoice → `invoice.created`.
2. **Notifications** emails invoice.
3. **Scheduler** nightly: overdue/approaching due → `invoice.reminder_due` → **Notifications**.

**Services:** Billing, Inventory (event source), Scheduler, Notifications.

### Phase X — Observability (every phase)

| Consumer | Purpose |
|----------|---------|
| **Platform Admin & Monitoring** | Real-time cross-tenant admin portal view |
| **Audit Trail & Log** | Immutable compliance/debug history |
| **Reporting Analytics Feeder** | BI / data warehouse |

All subscribe to key RabbitMQ events (`po.created`, `trip.started`, `grv.created`, etc.).

---

## Key RabbitMQ events (cross-phase)

| Event | Typical publisher | Typical consumers |
|-------|-------------------|-------------------|
| `user.created` | User Management | Notifications, Platform Admin, Audit, Analytics |
| `org.verified` | Organization Management | Notifications, Platform Admin |
| `po.created` | Inventory Management | Platform Admin, Audit, Analytics |
| `po.approved` | Inventory Management | Notifications, Platform Admin |
| `po.approval_sla_breached` | Scheduler/Job | Notifications |
| `shipment.ready_for_pickup` | Shipment Management | Trip & Tracking, Notifications |
| `trip.started` | Trip & Tracking | Notifications, Fuel & Expenses, Platform Admin |
| `trip.stop_recorded` | Messaging Inbound / Trip | Trip & Tracking, Roadside & Support |
| `fund_request.created` | Fuel & Expenses | Notifications |
| `grv.created` | Inventory Management | Billing, Trip & Tracking, Platform Admin |
| `invoice.created` | Billing & Payments | Notifications |
| `invoice.reminder_due` | Scheduler/Job | Notifications |

---

## Client architecture

All clients talk to **API Gateway** only. Microservices are client-agnostic.

### Angular web portal

- **Users:** Supplier admins, customer staff, finance, clearing agents, internal ops (admin portal).
- **Use cases:** Complex admin, PO/GRV workflows, fleet compliance, invoicing, reports.
- **Backend:** Inventory, Organization, Billing, Platform Admin, and most services.

### Flutter mobile apps (role-specific)

| App | Users | Key services |
|-----|-------|--------------|
| **Driver** | Drivers | Trip & Tracking, Fuel & Expenses |
| **Receiver** | Customer warehouse gate | Inventory (GRV), Trip & Tracking |
| **Ops/Admin** | Supplier managers | Trip live map, fuel approvals, notifications |

Features: GPS telemetry, stops, offline queue, QR scan (receiver), fund requests.

### WhatsApp (two-way)

| Direction | Service | Example |
|-----------|---------|---------|
| **Outbound** | Notifications (+ Integration API) | "Your order is on the way" on `trip.started` |
| **Inbound** | Messaging Inbound & Bot | Driver texts `1` → `trip.stop_recorded` |

---

## LDMS business flow (simplified)

1. Supplier registers and uploads products (Excel/CSV).
2. Supplier onboards customers.
3. Customer/supplier places order; payment (full, credit, lay-by, pay-on-delivery).
4. Supplier uploads transport documents; two-stage approval.
5. Clearing agent selected for cross-border.
6. Truck and driver selected (location-based availability).
7. Trip starts; SMS/email notifications; real-time GPS tracking.
8. Roadside: fuel stations, mechanics on map.
9. Border clearance logged with notifications.
10. Supplier sends funds to driver if needed.
11. Delivery: customer verifies (OTP), notes damage, sign-off.
12. Return trip tracked.

---

## Implementation notes

- **User vs Auth split:** profiles/roles in User Management; login/tokens in Authentication.
- **Inventory is large:** owns PO through GRV — do not split prematurely without architectural review.
- **Documents:** metadata + links only; binary files via storage utility/S3.
- **Location:** other services store `location_id`, not raw addresses.
- **Scheduler:** publishes events or calls APIs — does not own business logic.
- Details in this document may be refined during implementation.
