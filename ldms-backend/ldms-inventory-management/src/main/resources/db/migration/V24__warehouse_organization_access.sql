-- Explicit cross-organisation warehouse sharing

CREATE TABLE warehouse_organization_access (
    id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_location_id BIGINT NOT NULL,
    granted_organization_id BIGINT NOT NULL,
    access_level VARCHAR(50) NOT NULL DEFAULT 'READ' COMMENT 'READ | FULFILL',
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(150) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_org_access (warehouse_location_id, granted_organization_id),
    KEY idx_warehouse_access_org (granted_organization_id),
    CONSTRAINT fk_warehouse_access_location FOREIGN KEY (warehouse_location_id)
        REFERENCES warehouse_location (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Cross-org warehouse visibility and fulfilment grants';
