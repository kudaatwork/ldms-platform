-- ============================================================
-- V1: Trip Tracking Core Schema
-- Database: ldms_trip_tracking
-- ============================================================

CREATE TABLE IF NOT EXISTS trip
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_number              VARCHAR(50)   NOT NULL UNIQUE COMMENT 'Human-readable trip reference, e.g. TRP-20240611-0001',
    organization_id          BIGINT        NOT NULL COMMENT 'Supplier org that owns the trip',
    shipment_id              BIGINT        NOT NULL COMMENT 'FK to ldms_shipment_management.shipment',
    inventory_transfer_id    BIGINT        NULL COMMENT 'Denorm: FK to ldms_inventory_management.inventory_transfer',

    -- Fleet assignment (set on start)
    fleet_driver_id          BIGINT        NULL COMMENT 'FK to ldms_fleet_management.fleet_driver',
    fleet_asset_id           BIGINT        NULL COMMENT 'FK to ldms_fleet_management.fleet_asset',

    -- Status lifecycle
    status                   VARCHAR(50)   NOT NULL DEFAULT 'SCHEDULED'
        COMMENT 'SCHEDULED | IN_TRANSIT | ARRIVED | OTP_PENDING | DELIVERED | CANCELLED',

    -- Timestamps
    started_at               DATETIME(6)   NULL,
    arrived_at               DATETIME(6)   NULL,
    completed_at             DATETIME(6)   NULL,

    -- Delivery
    receiver_user_id         BIGINT        NULL COMMENT 'User who verified the OTP and confirmed delivery',

    -- Denormalised display fields for tracking UI
    from_warehouse_name      VARCHAR(200)  NULL,
    to_warehouse_name        VARCHAR(200)  NULL,
    product_name             VARCHAR(500)  NULL,

    -- Audit
    entity_status            VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE',
    created_at               DATETIME(6)   NOT NULL,
    created_by               VARCHAR(150)  NOT NULL,
    modified_at              DATETIME(6)   NULL,
    modified_by              VARCHAR(150)  NULL,

    INDEX idx_trip_org_status (organization_id, entity_status),
    INDEX idx_trip_number (trip_number),
    INDEX idx_trip_shipment (shipment_id),
    INDEX idx_trip_status (status, entity_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================

CREATE TABLE IF NOT EXISTS trip_event
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id             BIGINT        NOT NULL,
    event_type          VARCHAR(50)   NOT NULL
        COMMENT 'DEPARTED | ARRIVED_AT_BORDER | CHECKPOINT | ARRIVED | OTP_SENT | OTP_VERIFIED | DELIVERED | NOTE',
    event_time          DATETIME(6)   NOT NULL,
    latitude            DECIMAL(10,7) NULL,
    longitude           DECIMAL(10,7) NULL,
    notes               TEXT          NULL,
    recorded_by_user_id BIGINT        NULL,

    -- Audit
    entity_status       VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)   NOT NULL,
    created_by          VARCHAR(150)  NOT NULL,
    modified_at         DATETIME(6)   NULL,
    modified_by         VARCHAR(150)  NULL,

    CONSTRAINT fk_trip_event_trip FOREIGN KEY (trip_id) REFERENCES trip (id),
    INDEX idx_trip_event_trip (trip_id, entity_status),
    INDEX idx_trip_event_time (event_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================

CREATE TABLE IF NOT EXISTS delivery_otp
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id          BIGINT       NOT NULL,
    otp_code_hash    VARCHAR(255) NOT NULL COMMENT 'BCrypt hash of the 6-digit OTP',
    expires_at       DATETIME(6)  NOT NULL,
    verified_at      DATETIME(6)  NULL,
    sent_to_user_id  BIGINT       NOT NULL COMMENT 'User (receiver) the OTP was issued for',
    sent_at          DATETIME(6)  NOT NULL,

    -- Audit
    entity_status    VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at       DATETIME(6)  NOT NULL,
    created_by       VARCHAR(150) NOT NULL,
    modified_at      DATETIME(6)  NULL,
    modified_by      VARCHAR(150) NULL,

    CONSTRAINT fk_delivery_otp_trip FOREIGN KEY (trip_id) REFERENCES trip (id),
    INDEX idx_delivery_otp_trip (trip_id, entity_status),
    INDEX idx_delivery_otp_sent_user (sent_to_user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
