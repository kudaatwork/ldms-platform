-- Optional per-organisation fuel consumption / telemetry feature (off by default).

ALTER TABLE organization
    ADD COLUMN fuel_consumption_enabled BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'When true, org may use fuel telemetry, consumption tracking, and related billing actions';
