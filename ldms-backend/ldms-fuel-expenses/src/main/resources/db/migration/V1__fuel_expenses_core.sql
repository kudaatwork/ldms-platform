-- ============================================================
-- V1: Fuel Expenses Core Schema
-- Creates the fuel_session table for per-trip fuel tracking.
-- ============================================================

CREATE TABLE IF NOT EXISTS fuel_session (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    trip_id                     BIGINT          NOT NULL,
    organization_id             BIGINT          NOT NULL,
    fleet_asset_id              BIGINT,
    fleet_driver_id             BIGINT,
    shipment_id                 BIGINT,

    -- Tank / consumption snapshot
    tank_capacity_liters        DECIMAL(19, 2)  NOT NULL DEFAULT 400.00,
    fuel_remaining_liters       DECIMAL(19, 2)  NOT NULL DEFAULT 400.00,
    fuel_level_pct              DECIMAL(5, 2)   NOT NULL DEFAULT 100.00,
    consumption_rate_l_per_100km DECIMAL(6, 2)  NOT NULL DEFAULT 35.00,
    distance_travelled_km       DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,

    -- Last known GPS position (nullable until first location update arrives)
    last_latitude               DECIMAL(10, 7),
    last_longitude              DECIMAL(10, 7),

    status                      VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    moving                      TINYINT(1)      NOT NULL DEFAULT 0,

    entity_status               VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at                  DATETIME(6)     NOT NULL,
    created_by                  VARCHAR(150)    NOT NULL,
    modified_at                 DATETIME(6),
    modified_by                 VARCHAR(150),

    PRIMARY KEY (id),
    CONSTRAINT uq_fuel_session_trip_id UNIQUE (trip_id),

    INDEX idx_fuel_session_org_status       (organization_id, entity_status),
    INDEX idx_fuel_session_trip             (trip_id),
    INDEX idx_fuel_session_fleet_asset      (fleet_asset_id),
    INDEX idx_fuel_session_status           (status, entity_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
