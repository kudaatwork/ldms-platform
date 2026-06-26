-- Snapshot the billing tier on each usage charge record so subscription quota
-- consumption (milestone / messaging / tracking-day credits) can be counted per
-- billing period without depending on the action's current catalog tier.

ALTER TABLE usage_charge_record
    ADD COLUMN billing_tier VARCHAR(20) NULL AFTER action_code;

-- Backfill from the action catalog where a tier is configured.
UPDATE usage_charge_record u
    JOIN platform_action_charge c ON c.action_code = u.action_code
    SET u.billing_tier = c.billing_tier
    WHERE u.billing_tier IS NULL
      AND c.billing_tier IS NOT NULL;

-- Backfill known premium-tier action codes that may predate catalog tiering.
UPDATE usage_charge_record
    SET billing_tier = 'TRACKING'
    WHERE billing_tier IS NULL
      AND action_code IN ('TRIP_TRACK', 'GPS_PING', 'LIVE_MAP_SESSION');

UPDATE usage_charge_record
    SET billing_tier = 'MESSAGING'
    WHERE billing_tier IS NULL
      AND action_code IN ('NOTIFICATION_SMS', 'WHATSAPP_COMMAND');

CREATE INDEX idx_usage_charge_org_tier_created
    ON usage_charge_record (organization_id, billing_tier, created_at);
