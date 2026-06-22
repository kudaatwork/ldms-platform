-- Platform wallet usage and low-balance alerts.

INSERT INTO notification_template (
    template_key,
    description,
    channels,
    channel_delivery_enabled,
    email_subject,
    email_body_html,
    sms_body,
    in_app_title,
    in_app_body,
    is_active,
    entity_status,
    created_at,
    updated_at
)
VALUES (
    'PLATFORM_ACTION_CHARGED',
    'Prepaid wallet usage charge recorded for a platform action',
    JSON_ARRAY('EMAIL'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — platform usage {{actionDisplayName}}',
    '<!DOCTYPE html><html lang="en"><body style="font-family:Arial,sans-serif;color:#0f172a"><p>Hello,</p><p>A platform usage charge was recorded for <strong>{{organizationName}}</strong>.</p><ul><li><strong>Action:</strong> {{actionDisplayName}} ({{actionCode}})</li><li><strong>Amount:</strong> {{chargeFormatted}}</li><li><strong>Wallet balance:</strong> {{balanceAfterFormatted}}</li></ul><p>View full usage in Analytics → Platform usage &amp; changes.</p></body></html>',
    'LX wallet: {{actionDisplayName}} {{chargeFormatted}}. Balance {{balanceAfterFormatted}}.',
    'Platform usage charge',
    '{{actionDisplayName}} — {{chargeFormatted}} deducted. Balance {{balanceAfterFormatted}}.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'PLATFORM_WALLET_LOW_BALANCE',
    'Prepaid wallet balance at or below low-balance threshold',
    JSON_ARRAY('EMAIL'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — wallet balance low',
    '<!DOCTYPE html><html lang="en"><body style="font-family:Arial,sans-serif;color:#0f172a"><p>Hello,</p><p>Your LX platform wallet for <strong>{{organizationName}}</strong> is running low.</p><ul><li><strong>Balance:</strong> {{balanceAfterFormatted}}</li><li><strong>Alert threshold:</strong> {{thresholdFormatted}}</li></ul><p>Top up your wallet in Settings → Billing to avoid interrupted operations.</p></body></html>',
    'LX wallet low: balance {{balanceAfterFormatted}}. Please top up.',
    'Wallet balance low',
    'Balance {{balanceAfterFormatted}} — top up your prepaid wallet soon.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    channels = VALUES(channels),
    channel_delivery_enabled = VALUES(channel_delivery_enabled),
    email_subject = VALUES(email_subject),
    email_body_html = VALUES(email_body_html),
    sms_body = VALUES(sms_body),
    in_app_title = VALUES(in_app_title),
    in_app_body = VALUES(in_app_body),
    is_active = VALUES(is_active),
    updated_at = NOW(6);
