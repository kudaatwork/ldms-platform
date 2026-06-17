-- Per-vehicle maximum permitted speed (km/h) for telematics alerts and corridor compliance.
ALTER TABLE fleet_asset
    ADD COLUMN max_speed_kmh DECIMAL(6, 2) NULL COMMENT 'Maximum permitted speed in km/h; NULL = no limit configured' AFTER utilization_pct;
