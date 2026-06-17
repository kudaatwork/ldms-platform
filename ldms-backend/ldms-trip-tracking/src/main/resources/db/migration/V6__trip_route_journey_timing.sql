-- Accumulated transit vs waiting time for live journey analytics.
ALTER TABLE trip_route_plan
    ADD COLUMN transit_seconds BIGINT NOT NULL DEFAULT 0 COMMENT 'Seconds vehicle was moving along corridor' AFTER distance_travelled_km,
    ADD COLUMN waiting_seconds BIGINT NOT NULL DEFAULT 0 COMMENT 'Seconds halted, on break, or roadside hold' AFTER transit_seconds,
    ADD COLUMN timing_last_tick_at DATETIME(6) NULL COMMENT 'Last journey timing flush instant' AFTER waiting_seconds,
    ADD COLUMN timing_last_moving TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether last timing bucket was transit (1) or waiting (0)' AFTER timing_last_tick_at;
