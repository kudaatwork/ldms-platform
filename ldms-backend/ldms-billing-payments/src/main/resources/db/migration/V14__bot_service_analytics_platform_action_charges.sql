-- Bot service & analytics/reporting wallet charge categories for admin Platform billing UI.

UPDATE platform_action_charge
SET category = 'BOT_SERVICE',
    display_name = 'LDMS Assistant message',
    description = 'Each user message to the LDMS AI assistant (Help & Support or Bot service monitor)',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'HELP_BOT_MESSAGE'
  AND entity_status <> 'DELETED';

UPDATE platform_action_charge
SET category = 'BOT_SERVICE',
    display_name = 'WhatsApp bot command',
    description = 'Inbound WhatsApp command processed by the messaging bot',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'WHATSAPP_COMMAND'
  AND entity_status <> 'DELETED';

UPDATE platform_action_charge
SET category = 'ANALYTICS',
    display_name = 'Report export',
    description = 'Export analytics, usage, or operational reports (CSV/PDF)',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE action_code = 'REPORT_EXPORT'
  AND entity_status <> 'DELETED';

INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, billing_tier, active, entity_status, created_at, created_by)
VALUES
    ('BOT_SESSION_START', 'Start bot session', 'Open a new LDMS Assistant conversation session', 5, 'BOT_SERVICE', 'LIGHT', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('BOT_ANALYTICS_EXPORT', 'Bot analytics export', 'Export bot usage and conversation analytics from admin', 15, 'ANALYTICS', 'STANDARD', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('SHIPMENT_ANALYTICS_EXPORT', 'Shipment analytics export', 'Export shipment corridor analytics and KPI dashboards', 15, 'ANALYTICS', 'STANDARD', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('REVENUE_REPORT_EXPORT', 'Revenue report export', 'Export platform revenue and wallet earnings report (admin)', 45, 'ANALYTICS', 'HEAVY', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    billing_tier = VALUES(billing_tier),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';
