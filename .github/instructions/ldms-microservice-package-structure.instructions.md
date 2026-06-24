---
description: "Canonical LDMS microservice Java package structure (match ldms-user-management)"
applyTo: "ldms-backend/ldms-*/**/*.java"
---

# LDMS Microservice Package Structure

All LDMS backend microservices MUST follow the same layout as `ldms-user-management`.

## Base package

`projectlx.<domain>.<subdomain>` — **never** `projectlx.co.zw.<concatenatedName>`.

| Module | Base package | Application class |
|--------|--------------|-------------------|
| ldms-user-management | `projectlx.user.management` | `UserManagementApplication` |
| ldms-fleet-management | `projectlx.fleet.management` | `FleetManagementApplication` |
| ldms-inventory-management | `projectlx.inventory.management` | `InventoryManagementApplication` |
| ldms-billing-payments | `projectlx.billing.payments` | `BillingPaymentsApplication` |
| ldms-shipment-management | `projectlx.shipment.management` | `ShipmentManagementApplication` |
| ldms-trip-tracking | `projectlx.trip.tracking` | `TripTrackingApplication` |

Filesystem: `src/main/java/projectlx/<domain>/<subdomain>/` (3 levels under `projectlx`).

`scanBasePackages`: own base package + `projectlx.co.zw.shared_library`.

## Mandatory sub-packages

```
{basename}/
├── {Service}Application.java
├── business/
│   ├── auditable/{api,impl}     (when audit snapshots needed)
│   ├── config
│   ├── logic/{api,impl,support}
│   └── validator/{api,impl}     ← NOT validation
├── clients/
├── config/
├── db/migration/                (Flyway Java migrations)
├── model/
├── repository/{config,specification}
├── service/
│   ├── config
│   ├── processor/{api,impl}
│   └── rest/{frontend,system,backoffice}
├── tasks/{api,impl}             (scheduled jobs)
└── utils/
    ├── audit, config, dtos, enums, requests, responses, security, support
```

Service-specific extras (`events/handlers`, `batch/`, `exceptions/`) are allowed under the base package.

## Layering (zero exceptions)

Controller (`*Resource`) → ServiceProcessor → Service → Validator (in ServiceImpl) → Auditable (mutations) → Repository (reads).

- **ServiceProcessors** delegate to `*Service` only — never inject validators or auditables.
- **ServiceImpl** calls `*ServiceValidator` for request/id validation, then `*ServiceAuditable.create|update|delete` for all writes — never `repository.save` directly in domain services.
- **Auditable** (`business.auditable`): one pair per persisted entity — thin `repository.save` wrappers with `(entity, Locale, username)` signatures.
- **Validator** (`business.validator`): one pair per service domain — returns `ValidatorDto`, uses `I18Code` + `MessageService`.
- **BusinessConfig** (`business.config.BusinessConfig`): `@Bean` wiring for all auditables, validators, and services. No `@Service` on `*ServiceImpl`, `*ValidatorImpl`, or `*AuditableImpl`.
- REST classes: `{Entity}{Audience}Resource` in `service.rest.{frontend|system|backoffice}`
- Processors: `{Entity}ServiceProcessor` / `Impl` in `service.processor` (wired in `service.config.ServiceConfig`)
- DTOs: `utils.requests`, `utils.responses`, `utils.dtos` — not a top-level `dto` package

Orchestration/support services (status managers, outbox, idempotency) may skip auditable/validator when they have no REST request boundary.

## REST paths

`/ldms-{service-name}/v1/{frontend|system|backoffice}/{kebab-entity}`
