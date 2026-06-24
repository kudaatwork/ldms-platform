---
description: Write Flyway database migrations for LDMS microservices following strict conventions
applyTo: ldms-backend/**/src/main/resources/db/migration/*.sql
---

# Skill: Write a Flyway Migration for LDMS

Use this skill when creating or modifying Flyway migration files in any LDMS microservice.

## Prerequisites Check

1. Identify the target microservice (e.g. `ldms-inventory-management`).
2. Check existing migrations in `src/main/resources/db/migration/` to determine the next version number.
3. Verify the migration is **immutable** — never edit an already-deployed migration.

## Naming Convention

```
V{version}__{description}.sql
```

- **Version:** Sequential integer. Check existing files to find the next number.
- **Description:** Snake_case, descriptive, no spaces.
- **Examples:**
  - `V1__create_base_tables.sql`
  - `V2__add_purchase_order_tables.sql`
  - `V3__add_po_status_index.sql`
  - `V4__add_grv_line_items_table.sql`

## Migration Template

```sql
-- ================================================================
-- Migration: {Brief Description}
-- Version: V{n}
-- Service: ldms-{service-name}
-- Purpose: {What this migration accomplishes}
-- Dependencies: {Previous migrations or related entities}
-- Author: {Name}
-- Date: {YYYY-MM-DD}
-- ================================================================

-- ---------------------------------------------------------------
-- Table: {table_name}
-- ---------------------------------------------------------------
CREATE TABLE {table_name} (
    id BIGINT NOT NULL AUTO_INCREMENT,

    -- === BUSINESS FIELDS ===
    {field_name} VARCHAR(200) NOT NULL,
    {another_field} DECIMAL(19,4) DEFAULT 0.0000,
    {status_field} VARCHAR(50) NOT NULL,

    -- === FOREIGN KEYS ===
    organization_id BIGINT NOT NULL,
    {related_entity}_id BIGINT,

    -- === STATUS AND FLAGS ===
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- === AUDIT FIELDS ===
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------
CREATE INDEX idx_{table}_{field} ON {table_name}({field_name});
CREATE INDEX idx_{table}_org_status ON {table_name}(organization_id, entity_status);
CREATE UNIQUE INDEX ux_{table}_{unique_field} ON {table_name}({unique_field});

-- ---------------------------------------------------------------
-- Foreign Key Constraints (optional)
-- ---------------------------------------------------------------
ALTER TABLE {table_name}
    ADD CONSTRAINT fk_{table}_{referenced_table}
    FOREIGN KEY ({referenced_entity}_id)
    REFERENCES {referenced_table}(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- ---------------------------------------------------------------
-- Junction / Join Table (if needed)
-- ---------------------------------------------------------------
CREATE TABLE {join_table_name} (
    id BIGINT NOT NULL AUTO_INCREMENT,
    {parent}_id BIGINT NOT NULL,
    {child}_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_{join}_parent FOREIGN KEY ({parent}_id) REFERENCES {parent_table}(id),
    CONSTRAINT fk_{join}_child FOREIGN KEY ({child}_id) REFERENCES {child_table}(id),
    UNIQUE KEY ux_{join}_pair ({parent}_id, {child}_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Column Type Rules

| Concept | SQL Type | Java Type | Notes |
|---------|----------|-----------|-------|
| Primary key | `BIGINT NOT NULL AUTO_INCREMENT` | `Long` | Always |
| Money / amounts | `DECIMAL(19,4)` | `BigDecimal` | 4 decimal places |
| Quantities | `DECIMAL(19,2)` | `BigDecimal` | 2 decimal places |
| Timestamps | `DATETIME(6)` | `LocalDateTime` | Microsecond precision |
| Enums | `VARCHAR(50)` | `Enum` + `@Enumerated(STRING)` | Never MySQL ENUM |
| Status flags | `VARCHAR(50) NOT NULL` | `String` / Enum | e.g. `ACTIVE`, `DELETED` |
| Short text | `VARCHAR(200)` | `String` | Names, titles |
| Medium text | `VARCHAR(500)` | `String` | Descriptions |
| Long text | `TEXT` | `String` | Notes, comments |
| Boolean | `TINYINT(1)` | `Boolean` | 0 = false, 1 = true |
| JSON | `JSON` | `String` / DTO | For flexible data |

## Mandatory Fields on Every Table

Every table MUST include these columns:

```sql
entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
created_at DATETIME(6) NOT NULL,
created_by VARCHAR(100) NOT NULL,
modified_at DATETIME(6),
modified_by VARCHAR(100)
```

## Index Rules

1. Always index `organization_id` + `entity_status` for multi-tenant queries.
2. Index foreign key columns.
3. Use `UNIQUE` indexes for natural keys (e.g. `organization_id` + `code`).
4. Prefix: `idx_{table}_{field}` for regular, `ux_{table}_{field}` for unique.

## Foreign Key Rules

1. Always use `ON DELETE RESTRICT` — soft deletes only.
2. Always use `ON UPDATE CASCADE`.
3. Name: `fk_{table}_{referenced_table}`.

## What NOT to Do

- ❌ Never edit an already-deployed migration file.
- ❌ Never use MySQL `ENUM` type.
- ❌ Never use `DELETE` statements in migrations (soft delete via `entity_status`).
- ❌ Never skip the audit fields (`created_at`, `created_by`, etc.).
- ❌ Never use `FLOAT` or `DOUBLE` for money.
- ❌ Never create tables without `ENGINE=InnoDB`.

## Verification

After writing the migration, verify:

```bash
cd ldms-backend/ldms-{service-name}
mvn flyway:info    # Check migration status
mvn flyway:validate # Validate migrations
```

Or run the service Spring Boot app — Flyway auto-runs on startup when `ddl-auto: validate`.
