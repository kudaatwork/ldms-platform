-- Greet recipients by first name (falls back to username when first name is absent).
UPDATE notification_template
SET
    email_body_html = '<p>Hello {{firstName}},</p><p>Please verify your email by clicking <a href="{{verificationLink}}">this link</a>.</p><p>If you did not create this account, you can ignore this message.</p>',
    updated_at = NOW(6)
WHERE template_key = 'USER_REGISTRATION_VERIFICATION';

UPDATE notification_template
SET
    sms_body = 'Welcome {{firstName}}! Your Project LX account has been created.',
    updated_at = NOW(6)
WHERE template_key = 'NEW_USER_WELCOME_SMS';

UPDATE notification_template
SET
    in_app_body = 'Hi {{firstName}}, please verify your email to activate your account.',
    updated_at = NOW(6)
WHERE template_key = 'USER_REGISTRATION_IN_APP';
