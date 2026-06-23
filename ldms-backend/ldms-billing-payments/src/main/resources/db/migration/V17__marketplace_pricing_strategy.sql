-- Marketplace pricing strategy: milestone PAYG fees, free admin actions, stakeholder subscriptions.
-- See docs/PROJECT-LX-PLATFORM-PRICING-GUIDE.md

-- ── Included (USD 0) — bundled in subscription; never bottleneck adoption ─────
UPDATE platform_action_charge SET charge_cents = 0, billing_tier = 'INCLUDED', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code IN (
    'NOTIFICATION_EMAIL', 'NOTIFICATION_PUSH',
    'AUDIT_LOG_WRITE', 'API_INTEGRATION_CALL',
    'DOCUMENT_UPLOAD', 'DOCUMENT_SHARE',
    'FLEET_COMPLIANCE_UPLOAD', 'FLEET_VEHICLE_REGISTER', 'FLEET_DRIVER_HIRE',
    'INVENTORY_STOCK_RESERVE', 'INVENTORY_TRANSFER',
    'PROCUREMENT_PR_APPROVE', 'PROCUREMENT_QUOTE_SUBMIT',
    'PROCUREMENT_PO_CUSTOMER_APPROVE', 'PROCUREMENT_PO_SUPPLIER_APPROVE', 'PROCUREMENT_SO_APPROVE',
    'ORDER_CREATE', 'SHIPMENT_UPDATE', 'TRIP_ASSIGN_DRIVER',
    'ROADSIDE_INCIDENT',
    'BOT_SESSION_START', 'HELP_BOT_MESSAGE', 'HELP_SUPPORT_TICKET_OPEN', 'HELP_LIVE_CHAT_MESSAGE',
    'BOT_ANALYTICS_EXPORT', 'SHIPMENT_ANALYTICS_EXPORT',
    'REPORT_EXPORT', 'REVENUE_REPORT_EXPORT', 'ORG_CUSTOMER_REGISTER'
);

-- ── Milestone fees — high-ROI transactional moments ───────────────────────────
UPDATE platform_action_charge
SET charge_cents = 1000,
    billing_tier = 'MILESTONE',
    display_name = 'Trip booking fee',
    description = 'Supplier binds a truck and driver to an order (successful trip initiation)',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'TRIP_CREATE'
  AND entity_status <> 'DELETED';

UPDATE platform_action_charge
SET charge_cents = 750,
    billing_tier = 'MILESTONE',
    display_name = 'Trip completion fee',
    description = 'Trip marked complete with audit trail and proof of corridor execution',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'TRIP_COMPLETE'
  AND entity_status <> 'DELETED';

UPDATE platform_action_charge
SET charge_cents = 800,
    billing_tier = 'MILESTONE',
    display_name = 'Shipment dispatch fee',
    description = 'Shipment dispatch released to corridor execution',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'SHIPMENT_DISPATCH'
  AND entity_status <> 'DELETED';

UPDATE platform_action_charge
SET charge_cents = 500,
    billing_tier = 'MILESTONE',
    display_name = 'Goods received (GRV) fee',
    description = 'Proof of delivery recorded at destination',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'INVENTORY_GRV_CREATE'
  AND entity_status <> 'DELETED';

UPDATE platform_action_charge
SET charge_cents = 500,
    billing_tier = 'MILESTONE',
    display_name = 'Invoice generation fee',
    description = 'Customer invoice generated after delivery (GRV)',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'INVOICE_GENERATE'
  AND entity_status <> 'DELETED';

UPDATE platform_action_charge
SET charge_cents = 300,
    billing_tier = 'MILESTONE',
    display_name = 'Road fund transfer fee',
    description = 'Processing fee when emergency operational funds are sent to a driver on the road (percentage fee planned)',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'FUEL_FUND_REQUEST'
  AND entity_status <> 'DELETED';

INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, billing_tier, active, entity_status, created_at, created_by)
VALUES
    ('CLEARING_AGENT_MATCH', 'Clearing agent match fee', 'Supplier selects a clearing agent through the platform for cross-border clearance', 2500, 'LOGISTICS', 'MILESTONE', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ROAD_FUND_TRANSFER', 'Road fund transfer fee', 'Convenience fee for driver road-fund payout via integrated payment gateway', 300, 'LOGISTICS', 'MILESTONE', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    billing_tier = VALUES(billing_tier),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';

-- ── Premium GPS (USD 1.50 / trip-day) — hardware integration add-on ───────────
UPDATE platform_action_charge
SET charge_cents = 150,
    billing_tier = 'TRACKING',
    description = 'Premium high-frequency GPS hardware tracking (max once per trip per calendar day)',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code IN ('TRIP_TRACK', 'GPS_PING', 'LIVE_MAP_SESSION');

-- ── Fuel telemetry (USD 1.75 / vehicle-day) ───────────────────────────────────
UPDATE platform_action_charge
SET charge_cents = 175,
    billing_tier = 'TELEMETRY',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'FUEL_TELEMETRY_DAY'
  AND entity_status <> 'DELETED';

-- ── Messaging — telco pass-through after subscription SMS quota ───────────────
UPDATE platform_action_charge SET charge_cents = 10, billing_tier = 'MESSAGING', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code = 'NOTIFICATION_SMS';

UPDATE platform_action_charge SET charge_cents = 8, billing_tier = 'MESSAGING', modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE action_code = 'WHATSAPP_COMMAND';

-- ── Stakeholder subscription packages ─────────────────────────────────────────
UPDATE subscription_package
SET name = 'Supplier Pro',
    monthly_price_cents = 34900,
    included_heavy_credits = 15,
    included_standard_credits = 200,
    included_light_credits = 0,
    included_tracking_day_credits = 15,
    fuel_consumption_available = 0,
    description = 'For suppliers moving product through the corridor
Unlimited customer onboarding and bulk CSV product uploads
Delivery route analytics and corridor KPI dashboards
200 SMS notifications included monthly
15 trip milestone credits and 15 premium GPS days monthly
All document uploads, approvals, and status updates included — no per-action admin fees',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'STARTER'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET name = 'Fleet Manager Premium',
    monthly_price_cents = 64900,
    included_heavy_credits = 40,
    included_standard_credits = 500,
    included_light_credits = 0,
    included_tracking_day_credits = 45,
    fuel_consumption_available = 1,
    featured = 1,
    description = 'For transport companies and fleet operators
Priority visibility when suppliers search for available trucks
Driver performance scores and automated fleet maintenance tracking
500 SMS notifications included monthly
40 trip milestone credits and 45 premium GPS days monthly
Optional fuel telemetry at $1.75/vehicle-day',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'GROWTH'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET name = 'Clearing Agent Portal',
    monthly_price_cents = 54900,
    included_heavy_credits = 25,
    included_standard_credits = 300,
    included_light_credits = 0,
    included_tracking_day_credits = 20,
    fuel_consumption_available = 0,
    description = 'For clearing agents managing cross-border documentation
Dedicated multi-shipment border documentation pipelines
Corridor clearance analytics and compliance reporting
300 SMS notifications included monthly
25 clearing-match milestone credits included monthly
All routine document uploads and status updates included',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'ENTERPRISE'
  AND entity_status <> 'DELETED';
