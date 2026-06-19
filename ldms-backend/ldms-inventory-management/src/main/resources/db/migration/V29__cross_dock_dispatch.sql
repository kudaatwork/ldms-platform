-- Lightweight cross-docking dispatch record created when a third-party integration
-- submits a dispatch ingest for an org with cross_docking_enabled = true.
-- Triggers downstream shipment creation in shipment-management service.

CREATE TABLE cross_dock_dispatch (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id       BIGINT         NOT NULL,
    external_dispatch_id  VARCHAR(100)   NOT NULL COMMENT 'Caller-supplied external reference',
    product_id            BIGINT         NULL,
    external_product_id   VARCHAR(100)   NULL,
    product_code          VARCHAR(100)   NULL,
    quantity              DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    from_location_label   VARCHAR(200)   NULL,
    to_location_label     VARCHAR(200)   NULL,
    customer_reference    VARCHAR(200)   NULL,
    en_route_depot_labels TEXT           NULL COMMENT 'JSON array of en-route depot labels',
    status                VARCHAR(50)    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | DISPATCHED | COMPLETED | CANCELLED',
    shipment_id           BIGINT         NULL,
    shipment_number       VARCHAR(100)   NULL,
    integration_credential_id BIGINT     NOT NULL,
    entity_status         VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at            DATETIME(6)    NULL,
    created_by            VARCHAR(100)   NULL,
    modified_at           DATETIME(6)    NULL,
    modified_by           VARCHAR(100)   NULL,

    INDEX idx_cross_dock_org            (organization_id),
    INDEX idx_cross_dock_ext_dispatch   (external_dispatch_id),
    INDEX idx_cross_dock_status         (status, entity_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
