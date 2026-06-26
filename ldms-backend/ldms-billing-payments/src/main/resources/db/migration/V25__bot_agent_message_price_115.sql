-- LDMS Agent message: $1.15 per user message (autonomous tools + LLM).
INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, billing_tier, active, entity_status, created_at, created_by)
VALUES
    ('HELP_BOT_AGENT_MESSAGE', 'LDMS Agent message', 'Each user message in Agent mode — autonomous tools (wallet, navigation, support tickets, LDMS knowledge)', 115, 'BOT_SERVICE', 'LIGHT', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    billing_tier = VALUES(billing_tier),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';
