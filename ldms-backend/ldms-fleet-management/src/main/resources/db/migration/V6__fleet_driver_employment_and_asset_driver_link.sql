-- Driver employment model (employed vs pool) and persistent vehicle–driver link.
ALTER TABLE fleet_driver
    ADD COLUMN employment_type VARCHAR(50) NOT NULL DEFAULT 'EMPLOYED'
        COMMENT 'EMPLOYED = dedicated staff, POOL = hired pool assignable to any truck'
        AFTER organization_id;

ALTER TABLE fleet_asset
    ADD COLUMN fleet_driver_id BIGINT NULL AFTER driver_name,
    ADD KEY idx_fleet_asset_fleet_driver_id (fleet_driver_id);
