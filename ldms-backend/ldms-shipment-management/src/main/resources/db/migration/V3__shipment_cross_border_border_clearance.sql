-- Cross-border flag and border clearance workflow for international shipments.
ALTER TABLE shipment
    ADD COLUMN cross_border TINYINT(1) NOT NULL DEFAULT 0 AFTER quantity;

CREATE TABLE IF NOT EXISTS border_clearance_case (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    case_number             VARCHAR(50)     NOT NULL,
    organization_id         BIGINT          NOT NULL,
    shipment_id             BIGINT          NOT NULL,
    inventory_transfer_id   BIGINT          NOT NULL,
    trip_id                 BIGINT          NULL,
    border_name             VARCHAR(200)    NULL,
    status                  VARCHAR(50)     NOT NULL DEFAULT 'AWAITING_DOCUMENTS',
    notes                   TEXT            NULL,
    cleared_at              DATETIME(6)     NULL,
    cleared_by              VARCHAR(150)    NULL,
    entity_status           VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME(6)     NOT NULL,
    created_by              VARCHAR(150)    NOT NULL,
    modified_at             DATETIME(6)     NULL,
    modified_by             VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_border_clearance_case_number (case_number),
    KEY idx_bcc_shipment_id (shipment_id),
    KEY idx_bcc_org_status (organization_id, status, entity_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS border_clearance_document (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    case_id             BIGINT          NOT NULL,
    document_type       VARCHAR(50)     NOT NULL,
    file_upload_id      BIGINT          NOT NULL,
    file_name           VARCHAR(255)    NULL,
    description         VARCHAR(500)    NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    KEY idx_bcd_case_id (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
