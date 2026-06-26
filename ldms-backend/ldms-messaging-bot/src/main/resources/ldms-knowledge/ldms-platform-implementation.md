# LDMS — Platform implementation reference (Agent mode)

> For the LDMS Agent: how the system is built, which service owns what, and how clients call it.

## Microservice layout

Each backend service uses base package `projectlx.<domain>.<subdomain>` (e.g. `projectlx.user.management`, `projectlx.billing.payments`).

**Layering (mandatory):** REST `*Resource` → `*ServiceProcessor` → `*Service` → `*Validator` → `*Auditable` → `Repository`.

**REST paths:** `/ldms-{service-name}/v1/{frontend|system|backoffice}/{kebab-entity}`

All web/mobile clients call **API Gateway** only (port 8091 locally).

| Service | Owns |
|---------|------|
| User Management (#4) | Profiles, roles, groups — not login |
| Authentication (#5) | Login, JWT, password reset |
| Organization (#6) | Org profiles, KYC, verification |
| Inventory (#9) | Products, warehouses, stock, POs, GRVs |
| Shipment (#10) | Dispatch, docs, ready-for-release |
| Billing & Payments (#16) | Invoices, platform wallet, usage charges |
| Fleet (#12) | Trucks, trailers, driver compliance |
| Trip & Tracking (#13) | Trip lifecycle, GPS, stops |
| Fuel & Expenses (#15) | Driver fund requests |
| Notifications (#17) | Outbound email, SMS, WhatsApp |
| Messaging Inbound & Bot (#18) | WhatsApp inbound, LDMS Assistant chat |
| Location (#7) | Addresses, GPS (`location_id` refs) |
| Documents (#11) | Document metadata + file URLs |

## Event-driven integration

Use **RabbitTemplate** only. Event naming: `{entity}.{action}`.

| Event | Publisher | Meaning |
|-------|-----------|---------|
| `po.created` | Inventory | Customer PO saved, stock reserved |
| `po.approved` | Inventory | Supplier approved PO |
| `shipment.ready_for_pickup` | Shipment | Ready for trip assignment |
| `trip.started` | Trip & Tracking | Driver on corridor |
| `trip.stop_recorded` | Messaging Inbound / Trip | Stop logged |
| `grv.created` | Inventory | Goods received at destination |
| `invoice.created` | Billing | Invoice from GRV |
| `org.verified` | Organization | Org email/KYC verified |

Domain services publish events; **Notifications** sends comms; **Scheduler** triggers time-based jobs; **Platform Admin / Audit / Analytics** consume broadly.

## End-to-end ownership (who does what)

1. **PO → GRV:** Inventory owns the full order-to-receipt flow unless redesigned.
2. **Dispatch:** Inventory creates SO/dispatch → Shipment service creates shipment record.
3. **Trip:** Fleet assigns truck/driver; Trip & Tracking runs the corridor.
4. **Billing:** Platform wallet charges via Billing; milestone fees on trip booking, dispatch, GRV, invoice.
5. **Documents:** Returns `document_id`; owning service stores the link — not binary storage in domain DB.
6. **Location:** Other services store `location_id`, not raw addresses.

## Platform portal modules (Angular)

| Area | Typical path | Backend |
|------|--------------|---------|
| Inventory / orders | Orders workspace | Inventory |
| Fleet & tracking | Fleet module | Fleet, Trip |
| Settings / billing | Settings → Billing | Billing wallet |
| Help & Support | Help tab | Bot session API, support tickets |
| Users & groups | Users module | User Management |

## Supplier-registered org onboarding

When a supplier registers a customer/transporter: contact person gets temp credentials; org inbox gets email verification link. Contact must change credentials on first login (`mustChangeCredentials` in JWT).

## Agent mode

Agent mode uses an LLM **tool-calling loop** with seven tools:

- `get_session_context`, `get_wallet_summary`, `get_pricing_catalog`, `get_portal_navigation`
- `list_support_tickets`, `create_support_ticket`, `search_system_knowledge`

Tools call authenticated **system** APIs on behalf of the signed-in user (inter-service Feign). The agent must use tool results — not invent data.

See `ldms-portal-workflows.md` for end-to-end workflow tables and tool catalog.
