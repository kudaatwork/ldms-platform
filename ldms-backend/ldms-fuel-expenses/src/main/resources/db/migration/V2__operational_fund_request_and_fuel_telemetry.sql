-- ============================================================
-- V2: Operational Fund Request + Fuel Telemetry Log
-- ============================================================

-- ============================================================
-- Table: operational_fund_request
-- Stores driver fuel top-up / cash fund requests raised on trip.
-- Workflow: PENDING → APPROVED | REJECTED | CANCELLED
-- ============================================================

CREATE TABLE IF NOT EXISTS operational_fund_request (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    request_number      VARCHAR(50)     NOT NULL,
    trip_id             BIGINT          NOT NULL,
    organization_id     BIGINT          NOT NULL,
    fleet_driver_id     BIGINT          NOT NULL,
    fleet_asset_id      BIGINT,

    -- Request details
    request_type        VARCHAR(50)     NOT NULL,            -- FUEL_TOP_UP | FUNDS
    status              VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    liters_requested    DECIMAL(19, 2),
    amount_requested    DECIMAL(19, 4),
    currency_code       VARCHAR(10),
    latitude            DECIMAL(10, 7),
    longitude           DECIMAL(10, 7),
    driver_notes        VARCHAR(1000),

    -- Decision
    approved_liters     DECIMAL(19, 2),
    approved_amount     DECIMAL(19, 4),
    rejection_reason    VARCHAR(500),
    decided_by          VARCHAR(150),
    decided_at          DATETIME(6),

    -- Audit
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6),
    modified_by         VARCHAR(150),

    PRIMARY KEY (id),
    CONSTRAINT uq_operational_fund_request_number UNIQUE (request_number),

    INDEX idx_ofr_trip_id           (trip_id),
    INDEX idx_ofr_org_status        (organization_id, entity_status),
    INDEX idx_ofr_driver_status     (fleet_driver_id, status),
    INDEX idx_ofr_status_entity     (status, entity_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- Table: fuel_telemetry_log
-- Immutable audit log of all fuel readings from any source.
-- ============================================================

CREATE TABLE IF NOT EXISTS fuel_telemetry_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    fuel_session_id     BIGINT,
    trip_id             BIGINT          NOT NULL,
    organization_id     BIGINT          NOT NULL,
    fleet_asset_id      BIGINT,

    -- Source + type
    source              VARCHAR(50)     NOT NULL,            -- DRIVER_APP | TELEMATICS | SIMULATED | MANUAL | SYSTEM
    reading_type        VARCHAR(50)     NOT NULL,            -- LEVEL_SNAPSHOT | CONSUMPTION_DELTA | DISPENSE | TOP_UP

    -- Measurements (all optional — only relevant fields are populated per reading_type)
    fuel_level_pct      DECIMAL(5, 2),
    fuel_liters         DECIMAL(19, 2),
    odometer_km         DECIMAL(10, 2),
    latitude            DECIMAL(10, 7),
    longitude           DECIMAL(10, 7),
    distance_delta_km   DECIMAL(10, 4),
    consumed_liters     DECIMAL(19, 2),
    recorded_at         DATETIME(6)     NOT NULL,
    notes               VARCHAR(500),

    -- Audit
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6),
    modified_by         VARCHAR(150),

    PRIMARY KEY (id),

    INDEX idx_ftl_trip_id           (trip_id),
    INDEX idx_ftl_fuel_session      (fuel_session_id),
    INDEX idx_ftl_org_status        (organization_id, entity_status),
    INDEX idx_ftl_fleet_asset       (fleet_asset_id),
    INDEX idx_ftl_source_type       (source, reading_type),
    INDEX idx_ftl_recorded_at       (recorded_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
