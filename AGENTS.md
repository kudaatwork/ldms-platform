# Project LX LDMS — AI Agent Guide

> Logistics and Distribution Management System (LDMS) — a multi-tenant road-based logistics platform.
> **Full docs:** [`docs/LDMS-SYSTEM-ARCHITECTURE.md`](docs/LDMS-SYSTEM-ARCHITECTURE.md) · [`docs/PROJECT-LX-PLATFORM-PRICING-GUIDE.md`](docs/PROJECT-LX-PLATFORM-PRICING-GUIDE.md)

## Stack at a Glance

| Layer | Tech |
|-------|------|
| Backend | Java 17, Spring Boot 3.4.2, Maven multi-module |
| Database | MySQL 8.0 + Flyway migrations |
| Messaging | RabbitMQ (RabbitTemplate only — never MessageChannel) |
| Cache | Redis 7.2 + Redisson |
| API Gateway | Spring Cloud Gateway (port 8091) |
| Web | Angular 17+ (NgModule-based), Angular Material, SCSS |
| Mobile | Flutter 3.19+ + Riverpod |
| File Storage | Rust + Actix-Web 4 (port 8200) |
| IoT | MQTT (Mosquitto) + WebSocket |

## Build & Test Commands

### Backend
```bash
# From repo root
mvn -f ldms-backend/pom.xml clean package -DskipTests
mvn -f ldms-backend/pom.xml test

# Or from ldms-backend/
mvn clean package -DskipTests
mvn test
```
> **Maven settings:** `ldms-backend/.mvn/projectx-settings.xml` (enforced via `maven.config`). Requires Maven 3.9.2+.

### Angular (Admin Portal)
```bash
cd ldms-web/admin-portal
ng serve --port 4200        # Proxies /ldms-* → API Gateway :8091
ng test
ng build
```

### Angular (Platform Portal)
```bash
cd ldms-web/platform-portal
ng serve --port 4201        # Proxies /api → API Gateway :8091
ng test
ng build
```

### Flutter
```bash
cd ldms-mobile/driver-app    # or ops-app / receiver-app
flutter pub get
flutter run
```

## Critical Conventions (Zero Exceptions)

1. **Layering:** Controller → ServiceProcessor → Validator → Service → Repository. Never inject Service directly into Controller.
2. **Package base:** `projectlx.<domain>.<subdomain>` (e.g. `projectlx.user.management`). Never `projectlx.co.zw.<name>`.
3. **REST paths:** `/ldms-{service}/v1/{frontend|system|backoffice}/{kebab-entity}`
4. **DB enums:** `VARCHAR(50)` — never MySQL ENUM type.
5. **Money:** `DECIMAL(19,4)` in SQL · `BigDecimal` in Java.
6. **Timestamps:** `DATETIME(6)` in SQL · `LocalDateTime` in Java.
7. **Soft deletes:** `EntityStatus.DELETED` — never physical SQL DELETE.
8. **All entities:** `entity_status`, `created_at`, `created_by`, `modified_at`, `modified_by`.
9. **Java enums:** `@Enumerated(EnumType.STRING)`.
10. **Flyway:** `V{n}__{description}.sql` — sequential, immutable.
11. **RabbitMQ:** Use `RabbitTemplate` only. Event naming: `{entity}.{action}` (e.g. `po.created`, `trip.started`).
12. **Angular:** NgModule-based (not standalone). Use `lx-*` design system classes. No new UI dependencies.

## Service Boundaries (23 Microservices)

See [`docs/LDMS-SYSTEM-ARCHITECTURE.md`](docs/LDMS-SYSTEM-ARCHITECTURE.md) for full details.

| # | Service | Base Package | Status |
|---|---------|--------------|--------|
| 1 | API Gateway | — | ✅ Active |
| 2 | Config Server | — | ✅ Active |
| 3 | Eureka Server | — | ✅ Active |
| 4 | User Management | `projectlx.user.management` | ✅ Active |
| 5 | Authentication | `projectlx.authentication` | ✅ Active |
| 6 | Organization Management | `projectlx.organization.management` | ✅ Active |
| 7 | Locations | `projectlx.locations` | ✅ Active |
| 8 | Reference/Config | `projectlx.reference.config` | ⏸️ Commented out |
| 9 | Inventory Management | `projectlx.inventory.management` | ✅ Active |
| 10 | Shipment Management | `projectlx.shipment.management` | ✅ Active |
| 11 | Documents Management | `projectlx.documents.management` | ⏸️ Commented out |
| 12 | Fleet Management | `projectlx.fleet.management` | ✅ Active |
| 13 | Trip & Tracking | `projectlx.trip.tracking` | ✅ Active |
| 14 | Roadside & Support | `projectlx.roadside.support` | ⏸️ Commented out |
| 15 | Fuel & Expenses | `projectlx.fuel.expenses` | ✅ Active |
| 16 | Billing & Payments | `projectlx.billing.payments` | ✅ Active |
| 17 | Notifications | `projectlx.notifications` | ✅ Active |
| 18 | Messaging Inbound & Bot | `projectlx.messaging.bot` | ✅ Active |
| 19 | Platform Admin & Monitoring | `projectlx.platform.admin` | ⏸️ Commented out |
| 20 | Integration | `projectlx.integration` | ⏸️ Commented out |
| 21 | Reporting Analytics Feeder | `projectlx.reporting.analytics` | ⏸️ Commented out |
| 22 | Audit Trail | `projectlx.audit.trail` | ✅ Active |
| 23 | Scheduler/Job | `projectlx.scheduler` | ⏸️ Commented out |

> **Rule:** Only uncomment a `<module>` in `ldms-backend/pom.xml` after the subdirectory and its `pom.xml` exist. Re-comment if removed.

## Available Agents

Invoke these agents for domain-specific tasks:

| Agent | Purpose | File |
|-------|---------|------|
| `@backend-developer` | Spring Boot microservices, controllers, services, JPA | [`.github/agents/backend-developer.agent.md`](.github/agents/backend-developer.agent.md) |
| `@frontend-developer` | Angular components, services, routing, forms | [`.github/agents/frontend-developer.agent.md`](.github/agents/frontend-developer.agent.md) |
| `@ldms-ui-agent` | Angular UI components, dialogs, tables, SCSS theming | [`.github/agents/ldms-ui-agent.agent.md`](.github/agents/ldms-ui-agent.agent.md) |
| `@api-designer` | REST APIs, DTOs, OpenAPI, request/response objects | [`.github/agents/api-designer.agent.md`](.github/agents/api-designer.agent.md) |
| `@database-architect` | Flyway migrations, MySQL schema, indexes, constraints | [`.github/agents/database-architect.agent.md`](.github/agents/database-architect.agent.md) |
| `@event-handler` | RabbitMQ publishers, consumers, event-driven flows | [`.github/agents/event-handler.agent.md`](.github/agents/event-handler.agent.md) |
| `@mobile-developer` | Flutter screens, Riverpod, API clients, offline | [`.github/agents/mobile-developer.agent.md`](.github/agents/mobile-developer.agent.md) |
| `@test-engineer` | JUnit 5, MockMvc, integration tests, coverage | [`.github/agents/test-engineer.agent.md`](.github/agents/test-engineer.agent.md) |
| `@java-code-reviewer` | Review Java code before committing | [`.github/agents/java-code-reviewer.agent.md`](.github/agents/java-code-reviewer.agent.md) |

## Detailed Instructions

These files provide deep-dive guidance for specific areas:

| Topic | File |
|-------|------|
| Microservice package structure | [`.github/instructions/ldms-microservice-package-structure.instructions.md`](.github/instructions/ldms-microservice-package-structure.instructions.md) |
| System architecture (23 services, phases, events) | [`.github/instructions/ldms-system-architecture.instructions.md`](.github/instructions/ldms-system-architecture.instructions.md) |
| Platform pricing (wallet, subscriptions, billing) | [`.github/instructions/project-lx-platform-pricing.instructions.md`](.github/instructions/project-lx-platform-pricing.instructions.md) |
| Org onboarding (supplier/customer/transporter) | [`.github/instructions/platform-portal-org-onboarding.instructions.md`](.github/instructions/platform-portal-org-onboarding.instructions.md) |

## Local Dev Startup Order (for login)

| Order | Service | Port |
|-------|---------|------|
| 1 | MySQL | 3306 |
| 2 | ldms-user-management | 8086 |
| 3 | ldms-authentication | 8083 |
| 4 | ldms-api-gateway | 8091 |
| 5 | ng serve (admin portal) | 4200 |

## Common Pitfalls

- **Maven settings:** Do not rely on `~/.m2/settings.xml`. Use `ldms-backend/.mvn/projectx-settings.xml`.
- **IntelliJ Maven:** Ensure bundled Maven is 3.9.2+. The `-s .mvn/...` lone flag breaks IntelliJ argument parsing.
- **Gateway connection refused:** If gateway logs `Connection refused: /127.0.0.1:8083`, ldms-authentication is not running.
- **Angular proxy:** Admin portal proxies `/ldms-*` to gateway. Platform portal proxies `/api` to gateway. They are separate.
- **Module commenting:** Never add a service module to `pom.xml` before the directory and `pom.xml` exist.
