-- Help & Support wallet charges: LDMS Assistant (bot) vs live agent ticket chat.

INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, billing_tier, active, entity_status, created_at, created_by)
VALUES
    ('HELP_BOT_MESSAGE', 'LDMS Assistant message', 'Each user message sent to the LDMS AI assistant in Help & Support', 5, 'SUPPORT', 'LIGHT', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('HELP_SUPPORT_TICKET_OPEN', 'Support ticket opened', 'Open a live support ticket for agent follow-up', 15, 'SUPPORT', 'STANDARD', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('HELP_LIVE_CHAT_MESSAGE', 'Live support message', 'Each message in a support ticket conversation (customer or agent)', 25, 'SUPPORT', 'STANDARD', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    billing_tier = VALUES(billing_tier),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';
