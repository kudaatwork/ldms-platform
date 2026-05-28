-- Organisation notification templates: include SMS and WhatsApp for future use; only EMAIL delivers for now.
ALTER TABLE notification_template
    ADD COLUMN channel_delivery_enabled JSON NULL COMMENT 'Per-channel delivery flags, e.g. {"EMAIL":true,"SMS":false}';

UPDATE notification_template
SET
    channels = JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    channel_delivery_enabled = JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    sms_body = COALESCE(
        NULLIF(TRIM(sms_body), ''),
        'Project LX ({{organizationName}}): SMS notifications for this event are not active yet. Email is used for now.'
    ),
    whatsapp_template_name = COALESCE(
        NULLIF(TRIM(whatsapp_template_name), ''),
        'org_notification_placeholder'
    ),
    whatsapp_body = COALESCE(
        NULLIF(TRIM(whatsapp_body), ''),
        'Project LX ({{organizationName}}): WhatsApp notifications for this event are not active yet. Email is used for now.'
    ),
    updated_at = NOW(6)
WHERE template_key IN (
    'ORGANIZATION_REGISTERED_BY_ADMIN',
    'ORGANIZATION_SIGNUP_RECEIVED',
    'ORG_CONTACT_PERSON_VERIFICATION',
    'ORG_KYC_STAGE1_APPROVED',
    'ORG_KYC_STAGE2_APPROVED'
);
