-- Fleet registration notifications (email enabled; SMS/WhatsApp prepared for future activation).
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
    'ORG_FLEET_REGISTERED',
    'Notifies registering organisation and transport company when fleet is registered',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — fleet registered: {{registration}}',
    '<p>Hello {{contactName}},</p><p>{{fleetMessageIntro}}</p><p><strong>Registration:</strong> {{registration}}<br/><strong>Make/model:</strong> {{makeModel}}<br/><strong>Asset type:</strong> {{assetTypeLabel}}<br/><strong>Ownership:</strong> {{ownershipLabel}}<br/><strong>Registering organisation:</strong> {{registeringOrganizationName}}<br/><strong>Transport company:</strong> {{transporterName}}</p><p><strong>Performed by:</strong> {{performedBy}}</p><p>Sign in to review fleet: <a href="{{signInLink}}">{{signInLink}}</a></p>',
    NULL,
    'Fleet registered',
    'Fleet asset {{registration}} was registered for {{registeringOrganizationName}}.',
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
