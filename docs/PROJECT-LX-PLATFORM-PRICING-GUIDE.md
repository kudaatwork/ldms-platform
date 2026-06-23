# Project LX — Platform Pricing Guide

Canonical reference for LDMS monetisation. **Admins configure live rates in LX Admin → Settings → Platform billing.** This document defines strategy and default rate-card targets.

## Principles

1. **Multi-sided marketplace** — monetise suppliers, transport companies, and clearing agents; keep drivers and end-customers free to reduce friction.
2. **Subscriptions for power users** — wrap mundane admin (uploads, status updates, approvals) into predictable monthly plans.
3. **Pay-as-you-go for milestones** — charge at high-ROI moments: trip booking, dispatch, delivery proof, border clearance match, road-fund transfers.
4. **Never tax adoption** — do not charge per document upload, Excel import, or routine status update; that pushes users to WhatsApp/email.
5. **Premium hardware is optional** — basic phone/WhatsApp tracking is bundled; high-frequency GPS hardware and fuel telemetry are add-on per-day fees.

---

## 1. Pay-as-you-go (action-based)

### Milestone fees (transactional)

| Action | Default | When charged |
|--------|---------|--------------|
| Trip booking | **$10.00** | Supplier binds a truck/driver to an order (`TRIP_CREATE`) |
| Trip completed | **$7.50** | Successful trip closure (`TRIP_COMPLETE`) |
| Shipment dispatch | **$8.00** | Dispatch released (`SHIPMENT_DISPATCH`) |
| Goods received (GRV) | **$5.00** | Proof of delivery recorded (`INVENTORY_GRV_CREATE`) |
| Invoice generated | **$5.00** | Customer invoice after GRV (`INVOICE_GENERATE`) |
| Clearing agent match | **$25.00** | Supplier selects a clearing agent on platform (`CLEARING_AGENT_MATCH`) |
| Road fund transfer | **$3.00** flat + future % | Emergency funds to driver (`ROAD_FUND_TRANSFER` / `FUEL_FUND_REQUEST`) — target 1–2% of amount when % billing ships |

### Included in subscription (no per-action fee)

Documents, compliance uploads, stock reserve/transfer, procurement approvals, order create, shipment status updates, fleet registration, driver hire, assign driver, roadside incident logs, bot/support messages, report exports, customer org registration, email & push notifications.

### Premium add-ons

| Tier | Default | Covers |
|------|---------|--------|
| Premium GPS day | **$1.50 / trip-day** | Hardware GPS tracker connectivity (`TRIP_TRACK`, `GPS_PING`, `LIVE_MAP_SESSION`) — max once per trip per calendar day |
| Fuel telemetry day | **$1.75 / vehicle-day** | Fuel sensor hardware (`FUEL_TELEMETRY_DAY`) — max once per asset per calendar day |
| SMS / WhatsApp | **$0.10 / $0.08** | Telco pass-through + margin after free SMS quota in subscription |

---

## 2. Subscription tiers (business stakeholders)

Drivers and retail customers: **free**. Charge organisations extracting commercial value.

| Code | Persona | Target | Monthly (USD) | Key value |
|------|---------|--------|---------------|-----------|
| `STARTER` | **Supplier Starter** | Suppliers only | **$499** | PO/dispatch/GRV analytics, 100 SMS/mo, no fuel telemetry |
| `GROWTH` | **Corridor Growth** | Suppliers + transporters | **$899** | Fleet + supplier corridor ops, 450 SMS/mo, fuel telemetry optional |
| `ENTERPRISE` | **Enterprise Platform** | All stakeholders | **$1,299** | Full marketplace (supplier, transporter, clearing), 1,200 SMS/mo |

Included credits (marketing + future ledger): milestone trip credits, SMS bundle, premium GPS days — see Flyway seed in `V17__marketplace_pricing_strategy.sql`.

### Enterprise white-label (sales-led)

Not self-serve. Annual contract: upfront deployment + recurring cloud/support. Custom domain and branding for large fuel/gas distributors. Quote separately.

---

## 3. Value-add monetisation (roadmap)

- **SMS top-up packs** — after included monthly SMS quota.
- **Verified partner listings** — fuel stations and mechanics pay monthly to be featured on driver route maps.
- **Road fund % fee** — 1–2% on transfer amount via payment gateway (replace flat $3 when implemented).

---

## 4. Infrastructure cost reference (internal)

Use when tuning margins; do not expose per-device cost to customers.

| Item | Loaded monthly cost |
|------|---------------------|
| GPS tracker | ~$25/device (connectivity + hardware amortised) |
| Fuel sensor | ~$29/device |
| Twilio (platform) | ~$100 |
| AWS SES | ~$70 |
| GitHub | ~$25 |
| Server instance | ~$100/instance |

Premium GPS/telemetry day-rates should cover device costs at typical utilisation (~20 active days/month) with margin.

---

## 5. Anti-patterns

- Charging for document upload, compliance upload, or PO approval steps.
- Charging drivers or receivers for trip stops or GRV scanning.
- Flat per-ping GPS billing (use per trip-day cap instead).

---

## Implementation map

| Area | Location |
|------|----------|
| Rate card seed | `ldms-billing-payments` Flyway `V17__marketplace_pricing_strategy.sql` |
| Action codes | `PlatformWalletActionCodes` (shared library) |
| Billing tiers enum | `PlatformBillingTier` |
| Public pricing UI | Platform portal `platform-billing-tiers.util.ts`, landing pricing page |
| Admin tuning | LX Admin → Settings → Platform billing |
| Bot / help KB | `ldms-messaging-bot/.../platform-billing-charges.md` |

*Last aligned: marketplace milestone strategy (V17).*
