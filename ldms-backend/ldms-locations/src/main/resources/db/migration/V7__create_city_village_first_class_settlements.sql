-- Option A: first-class city and village tables.
-- Idempotent for resume after a partial apply (dev repair + re-migrate): guarded indexes/columns/FKs.
-- If Flyway history is still wrong, see: src/main/resources/db/manual/repair_after_failed_V7.sql
-- Idempotent: CREATE INDEX only when missing (never DROP indexes that may back InnoDB foreign keys).
-- Target hierarchy: Country → Province → District → City → (Suburb | Village) → Address.
-- Copies CITY / VILLAGE rows from location_node (preserves ids for a smooth transition).
-- Adds suburb.city_id and address.village_id / address.city_id. location_node is retained until
-- services switch to city/village APIs (follow-up migration can drop legacy columns).
-- Foreign keys on address settlement columns are deferred to a later migration once data is audited.

CREATE TABLE IF NOT EXISTS city (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(100) NULL,
    district_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    timezone VARCHAR(100) NULL,
    postal_code VARCHAR(30) NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(100) NULL,
    CONSTRAINT fk_city_district FOREIGN KEY (district_id) REFERENCES district (id)
);

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'city'
                AND index_name = 'idx_city_district'
        ) = 0,
        'CREATE INDEX idx_city_district ON city (district_id)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'city'
                AND index_name = 'idx_city_entity_status'
        ) = 0,
        'CREATE INDEX idx_city_entity_status ON city (entity_status)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

CREATE TABLE IF NOT EXISTS village (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(100) NULL,
    city_id BIGINT NOT NULL,
    district_id BIGINT NOT NULL,
    suburb_id BIGINT NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    timezone VARCHAR(100) NULL,
    postal_code VARCHAR(30) NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(100) NULL,
    CONSTRAINT fk_village_city FOREIGN KEY (city_id) REFERENCES city (id),
    CONSTRAINT fk_village_district FOREIGN KEY (district_id) REFERENCES district (id)
);

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'village'
                AND index_name = 'idx_village_city'
        ) = 0,
        'CREATE INDEX idx_village_city ON village (city_id)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'village'
                AND index_name = 'idx_village_district'
        ) = 0,
        'CREATE INDEX idx_village_district ON village (district_id)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'village'
                AND index_name = 'idx_village_entity_status'
        ) = 0,
        'CREATE INDEX idx_village_entity_status ON village (entity_status)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

INSERT IGNORE INTO city (
        id,
        name,
        code,
        district_id,
        latitude,
        longitude,
        timezone,
        postal_code,
        entity_status,
        created_at,
        created_by,
        modified_at,
        modified_by
    )
SELECT ln.id,
    ln.name,
    ln.code,
    ln.district_id,
    ln.latitude,
    ln.longitude,
    ln.timezone,
    ln.postal_code,
    ln.entity_status,
    ln.created_at,
    ln.created_by,
    ln.modified_at,
    ln.modified_by
FROM location_node ln
WHERE ln.location_type = 'CITY'
    AND ln.entity_status <> 'DELETED'
    AND ln.district_id IS NOT NULL;

INSERT IGNORE INTO village (
        id,
        name,
        code,
        city_id,
        district_id,
        suburb_id,
        latitude,
        longitude,
        timezone,
        postal_code,
        entity_status,
        created_at,
        created_by,
        modified_at,
        modified_by
    )
SELECT x.id,
    x.name,
    x.code,
    x.resolved_city_id,
    x.district_id,
    x.suburb_id,
    x.latitude,
    x.longitude,
    x.timezone,
    x.postal_code,
    x.entity_status,
    x.created_at,
    x.created_by,
    x.modified_at,
    x.modified_by
FROM (
        SELECT ln.id,
            ln.name,
            ln.code,
            ln.district_id,
            ln.suburb_id,
            ln.latitude,
            ln.longitude,
            ln.timezone,
            ln.postal_code,
            ln.entity_status,
            ln.created_at,
            ln.created_by,
            ln.modified_at,
            ln.modified_by,
            COALESCE(
                CASE
                    WHEN p.location_type = 'CITY'
                    AND p.entity_status <> 'DELETED' THEN p.id
                END,
                (
                    SELECT MIN(c2.id)
                    FROM city c2
                    WHERE c2.district_id = ln.district_id
                        AND c2.entity_status <> 'DELETED'
                )
            ) AS resolved_city_id
        FROM location_node ln
            LEFT JOIN location_node p ON p.id = ln.parent_id
        WHERE ln.location_type = 'VILLAGE'
            AND ln.entity_status <> 'DELETED'
            AND ln.district_id IS NOT NULL
            AND (
                EXISTS (
                    SELECT 1
                    FROM location_node p2
                    WHERE p2.id = ln.parent_id
                        AND p2.location_type = 'CITY'
                        AND p2.entity_status <> 'DELETED'
                )
                OR EXISTS (
                    SELECT 1
                    FROM city c
                    WHERE c.district_id = ln.district_id
                        AND c.entity_status <> 'DELETED'
                )
            )
    ) x
WHERE
    x.resolved_city_id IS NOT NULL;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
                AND table_name = 'suburb'
                AND column_name = 'city_id'
        ) = 0,
        'ALTER TABLE suburb ADD COLUMN city_id BIGINT NULL AFTER district_id',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'suburb'
                AND index_name = 'idx_suburb_city_id'
        ) = 0,
        'CREATE INDEX idx_suburb_city_id ON suburb (city_id)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

UPDATE suburb
SET
    city_id = city_location_node_id
WHERE
    city_location_node_id IS NOT NULL;

UPDATE suburb s
    JOIN (
        SELECT district_id,
            MIN(id) AS cid
        FROM city
        WHERE entity_status <> 'DELETED'
        GROUP BY district_id
    ) dc ON dc.district_id = s.district_id
SET
    s.city_id = dc.cid
WHERE
    s.city_id IS NULL;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
                AND table_name = 'address'
                AND column_name = 'village_id'
        ) = 0,
        'ALTER TABLE address ADD COLUMN village_id BIGINT NULL AFTER village_location_node_id',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE table_schema = DATABASE()
                AND table_name = 'address'
                AND column_name = 'city_id'
        ) = 0,
        'ALTER TABLE address ADD COLUMN city_id BIGINT NULL AFTER village_id',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'address'
                AND index_name = 'idx_address_village_id'
        ) = 0,
        'CREATE INDEX idx_address_village_id ON address (village_id)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

SET @sql_ldms_v7 = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
                AND table_name = 'address'
                AND index_name = 'idx_address_city_id'
        ) = 0,
        'CREATE INDEX idx_address_city_id ON address (city_id)',
        'SELECT 1'
    )
);

PREPARE stmt_ldms_v7 FROM @sql_ldms_v7;

EXECUTE stmt_ldms_v7;

DEALLOCATE PREPARE stmt_ldms_v7;

UPDATE address
SET
    village_id = village_location_node_id
WHERE
    village_location_node_id IS NOT NULL;

UPDATE address a
    JOIN suburb s ON s.id = a.suburb_id
SET
    a.city_id = s.city_id
WHERE
    a.suburb_id IS NOT NULL
    AND s.city_id IS NOT NULL;

-- MySQL rejects MODIFY on city.id while village.fk_village_city references it (errno 1833).
SET @drop_village_city_fk = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE constraint_schema = DATABASE()
                AND table_name = 'village'
                AND constraint_name = 'fk_village_city'
                AND constraint_type = 'FOREIGN KEY'
        ) > 0,
        'ALTER TABLE village DROP FOREIGN KEY fk_village_city',
        'SELECT 1'
    )
);

PREPARE stmt_drop_village_city_fk FROM @drop_village_city_fk;

EXECUTE stmt_drop_village_city_fk;

DEALLOCATE PREPARE stmt_drop_village_city_fk;

ALTER TABLE city
MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE village
MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

SET @add_village_city_fk = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM information_schema.TABLE_CONSTRAINTS
            WHERE constraint_schema = DATABASE()
                AND table_name = 'village'
                AND constraint_name = 'fk_village_city'
                AND constraint_type = 'FOREIGN KEY'
        ) = 0,
        'ALTER TABLE village ADD CONSTRAINT fk_village_city FOREIGN KEY (city_id) REFERENCES city (id)',
        'SELECT 1'
    )
);

PREPARE stmt_add_village_city_fk FROM @add_village_city_fk;

EXECUTE stmt_add_village_city_fk;

DEALLOCATE PREPARE stmt_add_village_city_fk;
