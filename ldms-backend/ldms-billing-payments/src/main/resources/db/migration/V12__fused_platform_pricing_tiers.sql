-- Fused Light / Standard / Heavy pay-as-you-go tiers, subscription realignment (infrastructure ~USD 400–500/org/mo).

ALTER TABLE platform_action_charge
    ADD COLUMN billing_tier VARCHAR(20) NULL AFTER category;

ALTER TABLE subscription_package
    ADD COLUMN included_heavy_credits INT NOT NULL DEFAULT 0 AFTER monthly_price_cents,
    ADD COLUMN included_standard_credits INT NOT NULL DEFAULT 0 AFTER included_heavy_credits,
    ADD COLUMN included_light_credits INT NOT NULL DEFAULT 0 AFTER included_standard_credits,
    ADD COLUMN included_tracking_day_credits INT NOT NULL DEFAULT 0 AFTER included_light_credits;

-- ── Light tier (USD 0.05) — workflow, documents, stock admin ─────────────────
UPDATE platform_action_charge SET charge_cents = 5, billing_tier = 'LIGHT', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN (
    'NOTIFICATION_EMAIL', 'NOTIFICATION_PUSH',
    'AUDIT_LOG_WRITE', 'API_INTEGRATION_CALL',
    'DOCUMENT_UPLOAD', 'DOCUMENT_SHARE',
    'FLEET_COMPLIANCE_UPLOAD',
    'INVENTORY_STOCK_RESERVE', 'INVENTORY_TRANSFER',
    'PROCUREMENT_PR_APPROVE', 'PROCUREMENT_QUOTE_SUBMIT',
    'PROCUREMENT_PO_CUSTOMER_APPROVE', 'PROCUREMENT_PO_SUPPLIER_APPROVE', 'PROCUREMENT_SO_APPROVE'
);

-- ── Standard tier (USD 0.15) — orders, fleet ops, corridor steps ───────────
UPDATE platform_action_charge SET charge_cents = 15, billing_tier = 'STANDARD', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN (
    'ORDER_CREATE', 'SHIPMENT_UPDATE',
    'FLEET_VEHICLE_REGISTER', 'FLEET_DRIVER_HIRE',
    'TRIP_ASSIGN_DRIVER', 'FUEL_FUND_REQUEST', 'ROADSIDE_INCIDENT'
);

-- ── Heavy tier (USD 0.45) — deliveries, proof, finance milestones ────────────
UPDATE platform_action_charge SET charge_cents = 45, billing_tier = 'HEAVY', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN (
    'TRIP_CREATE', 'TRIP_COMPLETE', 'SHIPMENT_DISPATCH',
    'INVENTORY_GRV_CREATE', 'INVOICE_GENERATE',
    'ORG_CUSTOMER_REGISTER', 'REPORT_EXPORT'
);

-- ── Tracking tier (USD 0.20 per trip-day; max once per trip per calendar day) ─
UPDATE platform_action_charge SET charge_cents = 20, billing_tier = 'TRACKING', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN ('TRIP_TRACK', 'GPS_PING', 'LIVE_MAP_SESSION');

-- ── Messaging tier (USD 0.07) — SMS / WhatsApp gateway cost + margin ─────────
UPDATE platform_action_charge SET charge_cents = 7, billing_tier = 'MESSAGING', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN ('NOTIFICATION_SMS', 'WHATSAPP_COMMAND');

-- Subscription packages: cover dedicated infrastructure (~USD 450/org) + included monthly usage credits.
UPDATE subscription_package
SET monthly_price_cents = 44900,
    included_heavy_credits = 100,
    included_standard_credits = 150,
    included_light_credits = 200,
    included_tracking_day_credits = 50,
    description = 'Small depot programmes (up to ~10 trucks)
Includes 100 Heavy · 150 Standard · 200 Light · 50 tracking-day credits monthly
Pay-as-you-go overage at published tier rates (Light $0.05 · Standard $0.15 · Heavy $0.45)
Dedicated platform infrastructure per organisation',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'STARTER'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET monthly_price_cents = 69900,
    included_heavy_credits = 500,
    included_standard_credits = 400,
    included_light_credits = 400,
    included_tracking_day_credits = 300,
    featured = 1,
    description = 'Mid-size corridor programmes (~10–40 trucks)
Includes 500 Heavy · 400 Standard · 400 Light · 300 tracking-day credits monthly
Priority LX operations support
10% bonus on wallet top-ups
Dedicated platform infrastructure per organisation',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'GROWTH'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET monthly_price_cents = 109900,
    included_heavy_credits = 1500,
    included_standard_credits = 1000,
    included_light_credits = 1000,
    included_tracking_day_credits = 800,
    description = 'Enterprise multi-depot operations
Includes 1,500 Heavy · 1,000 Standard · 1,000 Light · 800 tracking-day credits monthly
Dedicated customer success manager
Unlimited email and push notifications included
Dedicated platform infrastructure per organisation',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'ENTERPRISE'
  AND entity_status <> 'DELETED';
