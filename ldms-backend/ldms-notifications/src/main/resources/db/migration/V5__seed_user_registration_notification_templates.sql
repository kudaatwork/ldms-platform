-- Templates referenced by ldms-user-management UserServiceImpl on user create.
INSERT INTO notification_template (
    template_key,
    description,
    channels,
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
    'USER_REGISTRATION_VERIFICATION',
    'Email verification link sent when a user account is created',
    JSON_ARRAY('EMAIL'),
    'Verify your Project LX account',
    '<p>Hello {{userName}},</p><p>Please verify your email by clicking <a href="{{verificationLink}}">this link</a>.</p><p>If you did not create this account, you can ignore this message.</p>',
    NULL,
    NULL,
    NULL,
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'NEW_USER_WELCOME_SMS',
    'Welcome SMS for new users',
    JSON_ARRAY('SMS'),
    NULL,
    NULL,
    'Welcome {{userName}}! Your Project LX account has been created.',
    NULL,
    NULL,
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'USER_REGISTRATION_IN_APP',
    'In-app welcome notification (requires FCM token on recipient)',
    JSON_ARRAY('IN_APP'),
    NULL,
    NULL,
    NULL,
    'Welcome to Project LX',
    'Hi {{userName}}, please verify your email to activate your account.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    channels = VALUES(channels),
    email_subject = VALUES(email_subject),
    email_body_html = VALUES(email_body_html),
    sms_body = VALUES(sms_body),
    in_app_title = VALUES(in_app_title),
    in_app_body = VALUES(in_app_body),
    is_active = VALUES(is_active),
    updated_at = NOW(6);
