-- Agent mode: premium LDMS assistant with deep platform architecture knowledge (future tool actions).
INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, billing_tier, active, entity_status, created_at, created_by)
VALUES
    ('HELP_BOT_AGENT_MESSAGE', 'LDMS Agent message', 'Each user message in Agent mode — deep platform architecture knowledge and future action capabilities', 15, 'BOT_SERVICE', 'STANDARD', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    billing_tier = VALUES(billing_tier),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';
