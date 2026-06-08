-- SMS templates for phone OTP verification and login 2FA.

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
VALUES
(
    'PHONE_VERIFICATION_OTP',
    'One-time passcode sent to user''s phone number for phone number verification',
    JSON_ARRAY('SMS'),
    JSON_OBJECT('EMAIL', FALSE, 'SMS', TRUE, 'WHATSAPP', FALSE),
    NULL,
    NULL,
    'Your Project LX phone verification code is {{otp}}. It expires in 10 minutes. Do not share it with anyone.',
    'Verify your phone number',
    'Your verification code is {{otp}}. It expires in 10 minutes.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'LOGIN_2FA_OTP',
    'One-time passcode sent to user''s phone for two-factor authentication login challenge',
    JSON_ARRAY('SMS'),
    JSON_OBJECT('EMAIL', FALSE, 'SMS', TRUE, 'WHATSAPP', FALSE),
    NULL,
    NULL,
    'Your Project LX login verification code is {{otp}}. It expires in 10 minutes. If you did not attempt to log in, contact support immediately.',
    'Login verification code',
    'Your login verification code is {{otp}}. It expires in 10 minutes.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    description              = VALUES(description),
    channels                 = VALUES(channels),
    channel_delivery_enabled = VALUES(channel_delivery_enabled),
    sms_body                 = VALUES(sms_body),
    in_app_title             = VALUES(in_app_title),
    in_app_body              = VALUES(in_app_body),
    is_active                = VALUES(is_active),
    entity_status            = VALUES(entity_status),
    updated_at               = NOW(6);
