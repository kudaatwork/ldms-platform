-- Pause control, travelled distance, and GPS trail for live map / device-ready telemetry.
ALTER TABLE trip_route_plan
    ADD COLUMN simulation_paused TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'When 1, demo/device motion is halted (driver break / manual stop)' AFTER simulation_active,
    ADD COLUMN distance_travelled_km DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT 'Cumulative corridor distance travelled on this trip' AFTER overall_progress_pct,
    ADD COLUMN trail_json JSON NULL COMMENT 'Recent GPS trail [{latitude,longitude,recordedAt,speedKmh}] capped server-side' AFTER current_heading_deg;
