-- KYC rejection and resubmission notifications (email always; SMS/WhatsApp listed for future use).
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
    'ORG_KYC_REJECTED',
    'Notifies organisation and contact when a KYC application is rejected',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — KYC application not approved',
    '<p>Hello {{contactName}},</p><p>Your KYC application for <strong>{{organizationName}}</strong> was <strong>not approved</strong> at this time.</p><p><strong>Reason:</strong></p><p>{{rejectionReason}}</p><p>Please review the feedback, update your information and documents, and wait for instructions before resubmitting.</p><p>Platform: <a href="{{nextStepsLink}}">{{nextStepsLink}}</a><br/>Sign in: <a href="{{signInLink}}">{{signInLink}}</a></p>',
    NULL,
    'KYC application not approved',
    '{{organizationName}} was not approved. {{rejectionReason}}',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'ORG_KYC_RESUBMISSION_ALLOWED',
    'Notifies organisation and contact that they may submit a new KYC application',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — you may resubmit your KYC application',
    '<p>Hello {{contactName}},</p><p>Your organisation <strong>{{organizationName}}</strong> may <strong>submit a new KYC application</strong> on Project LX.</p><p><strong>Guidance from our team:</strong></p><p>{{resubmissionNotes}}</p><p>Sign in, review your details (you may reuse the same registration information), update anything that changed, and submit KYC again when ready.</p><p>Continue: <a href="{{nextStepsLink}}">{{nextStepsLink}}</a><br/>Sign in: <a href="{{signInLink}}">{{signInLink}}</a></p>',
    NULL,
    'KYC resubmission opened',
    'You may submit a new KYC application for {{organizationName}}.',
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
