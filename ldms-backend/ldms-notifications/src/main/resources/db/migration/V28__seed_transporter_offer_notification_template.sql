-- Email template for a supplier's transporter contract offer (awaiting acceptance).
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
    'ORG_TRANSPORTER_OFFER',
    'Notifies a transporter that a supplier has offered a transportation contract (awaiting acceptance)',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{supplierName}} would like to contract {{organizationName}} for transportation',
    '<p>Hello {{contactName}},</p><p><strong>{{supplierName}}</strong> has invited your organisation <strong>{{organizationName}}</strong> to provide transportation services on Project LX LDMS.</p><p>Sign in to review the offer and accept or decline it from your <strong>Connection requests</strong> page: <a href="{{signInLink}}">{{signInLink}}</a></p>',
    NULL,
    'New transporter contract offer',
    '{{supplierName}} has invited you to provide transportation services. Review it under Connection requests.',
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
