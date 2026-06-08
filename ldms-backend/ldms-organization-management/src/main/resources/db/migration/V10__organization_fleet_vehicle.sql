-- Own fleet vehicles owned by an organisation
CREATE TABLE organization_fleet_vehicle (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    organization_id     BIGINT          NOT NULL,
    registration        VARCHAR(50)     NOT NULL,
    make_model          VARCHAR(200)    NOT NULL,
    vehicle_type        VARCHAR(50)     NOT NULL COMMENT 'rig, van, tanker, flatbed',
    status              VARCHAR(50)     NOT NULL DEFAULT 'available' COMMENT 'available, on_road, yard, maintenance',
    driver_name         VARCHAR(150)    NULL,
    utilization_pct     DECIMAL(19,2)   NOT NULL DEFAULT 0.00,
    last_trip_at        DATETIME(6)     NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,

    CONSTRAINT fk_fleet_vehicle_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    INDEX idx_fleet_vehicle_org_status (organization_id, entity_status),
    INDEX idx_fleet_vehicle_registration (registration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
