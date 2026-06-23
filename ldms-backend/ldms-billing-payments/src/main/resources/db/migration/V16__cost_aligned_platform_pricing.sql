-- Cost-aligned platform pricing (GPS $25/mo, fuel sensor $29/mo, Twilio/SES/GitHub/servers overhead).
-- Target ~45–55% gross margin on marginal tiers; subscriptions cover dedicated infra + included credits.

-- ── Light tier (USD 0.08) — workflow, documents, email/push, bot ─────────────
UPDATE platform_action_charge SET charge_cents = 8, billing_tier = 'LIGHT', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN (
    'NOTIFICATION_EMAIL', 'NOTIFICATION_PUSH',
    'AUDIT_LOG_WRITE', 'API_INTEGRATION_CALL',
    'DOCUMENT_UPLOAD', 'DOCUMENT_SHARE',
    'FLEET_COMPLIANCE_UPLOAD',
    'INVENTORY_STOCK_RESERVE', 'INVENTORY_TRANSFER',
    'PROCUREMENT_PR_APPROVE', 'PROCUREMENT_QUOTE_SUBMIT',
    'PROCUREMENT_PO_CUSTOMER_APPROVE', 'PROCUREMENT_PO_SUPPLIER_APPROVE', 'PROCUREMENT_SO_APPROVE',
    'BOT_SESSION_START', 'HELP_BOT_MESSAGE'
);

-- ── Standard tier (USD 0.25) — orders, fleet ops, corridor steps, analytics ─
UPDATE platform_action_charge SET charge_cents = 25, billing_tier = 'STANDARD', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN (
    'ORDER_CREATE', 'SHIPMENT_UPDATE',
    'FLEET_VEHICLE_REGISTER', 'FLEET_DRIVER_HIRE',
    'TRIP_ASSIGN_DRIVER', 'FUEL_FUND_REQUEST', 'ROADSIDE_INCIDENT',
    'HELP_SUPPORT_TICKET_OPEN', 'HELP_LIVE_CHAT_MESSAGE',
    'BOT_ANALYTICS_EXPORT', 'SHIPMENT_ANALYTICS_EXPORT'
);

-- ── Heavy tier (USD 0.75) — deliveries, proof, finance milestones ───────────
UPDATE platform_action_charge SET charge_cents = 75, billing_tier = 'HEAVY', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN (
    'TRIP_CREATE', 'TRIP_COMPLETE', 'SHIPMENT_DISPATCH',
    'INVENTORY_GRV_CREATE', 'INVOICE_GENERATE',
    'ORG_CUSTOMER_REGISTER', 'REPORT_EXPORT', 'REVENUE_REPORT_EXPORT'
);

-- ── Tracking tier (USD 1.50 per trip-day; GPS tracker ~USD 25/mo loaded cost) ─
UPDATE platform_action_charge SET charge_cents = 150, billing_tier = 'TRACKING', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN ('TRIP_TRACK', 'GPS_PING', 'LIVE_MAP_SESSION');

-- ── Messaging tier (USD 0.12) — Twilio SMS / WhatsApp gateway + margin ────────
UPDATE platform_action_charge SET charge_cents = 12, billing_tier = 'MESSAGING', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN ('NOTIFICATION_SMS', 'WHATSAPP_COMMAND');

-- ── Telemetry tier (USD 1.75 per vehicle-day; fuel sensor ~USD 29/mo loaded) ─
INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, billing_tier, active, entity_status, created_at, created_by)
VALUES
    ('FUEL_TELEMETRY_DAY', 'Fuel telemetry day', 'Per vehicle per calendar day when fuel consumption telemetry is enabled (max once per asset per day)', 175, 'IOT', 'TELEMETRY', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    billing_tier = VALUES(billing_tier),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';

-- Subscription packages: server instance (~USD 100/org) + platform overhead + included usage credits.
UPDATE subscription_package
SET monthly_price_cents = 59900,
    included_heavy_credits = 60,
    included_standard_credits = 100,
    included_light_credits = 120,
    included_tracking_day_credits = 35,
    description = 'Small depot programmes (up to ~10 trucks)
Includes 60 Heavy · 100 Standard · 120 Light · 35 tracking-day credits monthly
Pay-as-you-go overage at published tier rates (Light $0.08 · Standard $0.25 · Heavy $0.75 · Tracking $1.50/day)
Dedicated platform infrastructure per organisation
GPS live tracking included — fuel consumption not included',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'STARTER'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET monthly_price_cents = 99900,
    included_heavy_credits = 350,
    included_standard_credits = 300,
    included_light_credits = 300,
    included_tracking_day_credits = 180,
    featured = 1,
    description = 'Mid-size corridor programmes (~10–40 trucks)
Includes 350 Heavy · 300 Standard · 300 Light · 180 tracking-day credits monthly
Optional fuel consumption & telemetry at $1.75/vehicle-day (enable in Settings)
Priority LX operations support
10% bonus on wallet top-ups
Dedicated platform infrastructure per organisation',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'GROWTH'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET monthly_price_cents = 149900,
    included_heavy_credits = 1000,
    included_standard_credits = 700,
    included_light_credits = 700,
    included_tracking_day_credits = 450,
    description = 'Enterprise multi-depot operations
Includes 1,000 Heavy · 700 Standard · 700 Light · 450 tracking-day credits monthly
Optional fuel consumption & telemetry at $1.75/vehicle-day (enable in Settings)
Dedicated customer success manager
Unlimited email and push notifications included
Dedicated platform infrastructure per organisation',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'ENTERPRISE'
  AND entity_status <> 'DELETED';
