-- ========================================================================
-- Flyway Migration V31: Organisation departments for purchase requisitions
-- ========================================================================

CREATE TABLE department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    department_code VARCHAR(60) NULL,
    description VARCHAR(500) NULL,
    supplier_id BIGINT NOT NULL COMMENT 'Owning organisation (FK to Org Service)',
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255) NULL,
    modified_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_org_name (supplier_id, name),
    UNIQUE KEY uk_department_org_code (supplier_id, department_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Organisation departments used on purchase requisitions';

CREATE INDEX idx_department_supplier ON department (supplier_id);

-- ========================================================================
-- END OF V31 MIGRATION
-- ========================================================================
