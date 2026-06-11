-- ============================================================
-- V1: Shipment core table
-- ============================================================
-- Shipment links an approved inventory transfer to fleet
-- allocation and (later) a trip.
-- ============================================================

CREATE TABLE IF NOT EXISTS shipment (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    shipment_number             VARCHAR(50)     NOT NULL,
    organization_id             BIGINT          NOT NULL,
    source_type                 VARCHAR(50)     NOT NULL,
    inventory_transfer_id       BIGINT          NOT NULL,
    from_warehouse_location_id  BIGINT          NULL,
    to_warehouse_location_id    BIGINT          NULL,
    from_warehouse_name         VARCHAR(200)    NULL,
    to_warehouse_name           VARCHAR(200)    NULL,
    product_id                  BIGINT          NULL,
    product_name                VARCHAR(200)    NULL,
    product_code                VARCHAR(60)     NULL,
    quantity                    DECIMAL(19,4)   NOT NULL DEFAULT 0.0000,
    fleet_driver_id             BIGINT          NULL,
    fleet_asset_id              BIGINT          NULL,
    trip_id                     BIGINT          NULL,
    status                      VARCHAR(50)     NOT NULL DEFAULT 'PENDING_ALLOCATION',
    notes                       TEXT            NULL,
    entity_status               VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at                  DATETIME(6)     NOT NULL,
    created_by                  VARCHAR(150)    NOT NULL,
    modified_at                 DATETIME(6)     NULL,
    modified_by                 VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_shipment_number (shipment_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_shipment_org_id         ON shipment (organization_id);
CREATE INDEX idx_shipment_transfer_id    ON shipment (inventory_transfer_id);
CREATE INDEX idx_shipment_status         ON shipment (status, entity_status);
