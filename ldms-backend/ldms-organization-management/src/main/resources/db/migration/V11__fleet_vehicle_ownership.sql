-- Distinguish organisation-owned assets from vehicles operated under a contracted transporter link.
ALTER TABLE organization_fleet_vehicle
    ADD COLUMN ownership_type VARCHAR(50) NOT NULL DEFAULT 'OWNED' COMMENT 'OWNED, CONTRACTED' AFTER organization_id,
    ADD COLUMN contracted_transporter_organization_id BIGINT NULL AFTER ownership_type,
    ADD CONSTRAINT fk_fleet_vehicle_contracted_transporter
        FOREIGN KEY (contracted_transporter_organization_id) REFERENCES organization (id),
    ADD INDEX idx_fleet_vehicle_contracted_transporter (contracted_transporter_organization_id, entity_status);
