# LDMS — End-to-end portal workflows (Agent reference)

> Agent tool companion: map user goals to services, events, and portal screens.

## Purchase order lifecycle (Inventory #9)

| Step | User action (portal) | Service | Event |
|------|---------------------|---------|-------|
| 1 | Customer creates PO | Inventory | `po.created` |
| 2 | Supplier approves PO | Inventory | `po.approved` |
| 3 | SLA breach (scheduler) | Inventory | `po.approval_sla_breached` |
| 4 | Reserve stock / dispatch | Inventory → Shipment | `shipment.ready_for_pickup` |

**Supplier path:** Inventory → Orders workspace  
**Customer path:** My Orders → create / track PO

## Shipment & dispatch (Shipment #10)

- Supplier creates dispatch / sales order from approved PO.
- Shipment service owns shipment record, documents checklist, ready-for-release.
- Billing may gate release on payment status.

**Portal:** Shipment Management → workspace tabs by role.

## Trip & corridor (Fleet #12 + Trip #13)

| Step | Trigger | Event |
|------|---------|-------|
| Assign truck/driver | Fleet ops | compliance check |
| Start trip | Driver app / ops | `trip.started` |
| Record stop | Driver / WhatsApp | `trip.stop_recorded` |
| Deliver / GRV | Receiver app | `grv.created` |

**Portal:** Fleet → tracking subnav; mobile apps for drivers/receivers.

## Billing & platform wallet (Billing #16)

- Prepaid wallet per organisation; usage charges per platform action.
- Subscription packages include SMS/WhatsApp quota meters.
- Agent tools: `get_wallet_summary`, `get_pricing_catalog`.

**Portal:** Settings → Billing (`/settings?section=billing`)

## Organisation onboarding

| Track | Recipient | Outcome |
|-------|-----------|---------|
| Supplier registers customer/transporter | Contact person | Temp credentials + `mustChangeCredentials` |
| Same flow | Org email | Verification link → `org.verified` |

See `platform-org-onboarding.md` for full detail.

## Help & Support

- **Assistant mode:** FAQ + knowledge RAG (included).
- **Agent mode:** tool loop — session context, navigation, wallet, tickets, knowledge search.
- Agent can **create_support_ticket** when ops must intervene.

## Agent tool catalog

| Tool | Purpose |
|------|---------|
| `get_session_context` | User, org, classification |
| `get_wallet_summary` | Balance, billing mode, package |
| `get_pricing_catalog` | Per-action wallet charges |
| `get_portal_navigation` | Menu paths by classification |
| `list_support_tickets` | User's open tickets |
| `create_support_ticket` | Open ticket for human follow-up |
| `search_system_knowledge` | Architecture, FAQ, PDF RAG |

## Classification → primary modules

| Classification | Primary workspaces |
|----------------|-------------------|
| SUPPLIER | Inventory, Shipment, Fleet, Customers |
| CUSTOMER | My Orders, Shipments, Invoices |
| TRANSPORT_COMPANY | Fleet, Shipment |
| CLEARING_AGENT | Shipment (clearance) |
| SERVICE_STATION | Truck visits, Fuel log |
| ROADSIDE_SUPPORT_SERVICE | Incidents, Service log |
| GOVERNMENT_AGENCY | Border activity |
