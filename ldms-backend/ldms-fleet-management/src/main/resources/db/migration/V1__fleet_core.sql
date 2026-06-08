-- ================================================================
-- Service: ldms-fleet-management
-- Version: V1
-- Purpose: Core fleet schema — physical assets (trucks/trailers),
--          drivers linked to user-management, and compliance records
--          referencing file-upload-service documents with expiry dates.
-- Notes: organization_id, user_id, and file_upload_id are logical
--        references only (no cross-database foreign keys).
-- ================================================================

CREATE TABLE fleet_asset (
    id                                      BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id                         BIGINT          NOT NULL,
    asset_type                              VARCHAR(50)     NOT NULL COMMENT 'TRUCK, TRAILER, RIG, VAN, TANKER, FLATBED',
    ownership_type                          VARCHAR(50)     NOT NULL DEFAULT 'OWNED' COMMENT 'OWNED, CONTRACTED',
    contracted_transporter_organization_id  BIGINT          NULL,
    registration                            VARCHAR(50)     NOT NULL,
    make_model                              VARCHAR(200)    NOT NULL,
    status                                  VARCHAR(50)     NOT NULL DEFAULT 'available' COMMENT 'available, on_road, yard, maintenance',
    driver_name                             VARCHAR(150)    NULL,
    utilization_pct                         DECIMAL(19,2)   NOT NULL DEFAULT 0.00,
    entity_status                           VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at                              DATETIME(6)     NOT NULL,
    created_by                              VARCHAR(150)    NOT NULL,
    modified_at                             DATETIME(6)     NULL,
    modified_by                             VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    KEY idx_fleet_asset_org_status (organization_id, entity_status),
    KEY idx_fleet_asset_registration (registration),
    KEY idx_fleet_asset_asset_type (asset_type, entity_status),
    KEY idx_fleet_asset_ownership (organization_id, ownership_type, entity_status),
    KEY idx_fleet_asset_contracted_transporter (contracted_transporter_organization_id, entity_status),
    KEY idx_fleet_asset_status (organization_id, status, entity_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fleet_driver (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id     BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL COMMENT 'Logical reference to ldms_user_management.user',
    first_name          VARCHAR(100)    NOT NULL,
    last_name           VARCHAR(100)    NOT NULL,
    phone_number        VARCHAR(50)     NULL,
    license_number      VARCHAR(100)    NULL,
    license_class       VARCHAR(50)     NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_fleet_driver_org_user (organization_id, user_id),
    KEY idx_fleet_driver_org_status (organization_id, entity_status),
    KEY idx_fleet_driver_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fleet_compliance_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id     BIGINT          NOT NULL,
    subject_type        VARCHAR(50)     NOT NULL COMMENT 'ASSET, DRIVER',
    subject_id          BIGINT          NOT NULL COMMENT 'fleet_asset.id or fleet_driver.id',
    compliance_type     VARCHAR(50)     NOT NULL COMMENT 'INSURANCE, LICENSE, MAINTENANCE, ROADWORTHINESS',
    file_upload_id      BIGINT          NOT NULL COMMENT 'Logical reference to ldms_file_upload.file_upload',
    expires_at          DATETIME(6)     NOT NULL,
    status              VARCHAR(50)     NOT NULL DEFAULT 'PENDING' COMMENT 'VALID, EXPIRING_SOON, EXPIRED, PENDING',
    notes               VARCHAR(500)    NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    KEY idx_fleet_compliance_org_status (organization_id, entity_status),
    KEY idx_fleet_compliance_subject (organization_id, subject_type, subject_id, entity_status),
    KEY idx_fleet_compliance_type (organization_id, compliance_type, entity_status),
    KEY idx_fleet_compliance_status (organization_id, status, entity_status),
    KEY idx_fleet_compliance_expires (organization_id, expires_at, entity_status),
    KEY idx_fleet_compliance_file (file_upload_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
