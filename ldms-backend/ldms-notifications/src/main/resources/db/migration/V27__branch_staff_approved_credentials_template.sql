-- Branch staff approved credentials notification template.
-- Sent to a branch clerk or branch manager when their platform user account is created
-- from the workforce dialogs (single create or bulk import). Email channel only.

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
    'BRANCH_STAFF_APPROVED_CREDENTIALS',
    'Temporary login credentials sent to a newly registered branch clerk or manager',
    JSON_ARRAY('EMAIL'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    'Welcome to Project LX — Your Login Credentials',
    '<p>Hello {{firstName}},</p>'
        '<p>An account has been created for you on the Project LX platform.</p>'
        '<p>Use the following temporary credentials to sign in. You will be prompted to set your '
        'own permanent username and password on first login:</p>'
        '<table style="border-collapse:collapse;margin:12px 0">'
        '<tr><td style="padding:4px 12px 4px 0;font-weight:bold;">Username:</td>'
        '<td style="padding:4px 0;">{{temporaryUsername}}</td></tr>'
        '<tr><td style="padding:4px 12px 4px 0;font-weight:bold;">Password:</td>'
        '<td style="padding:4px 0;">{{temporaryPassword}}</td></tr>'
        '</table>'
        '<p><a href="{{signInLink}}" '
        'style="display:inline-block;padding:10px 24px;background:#1565C0;color:#fff;text-decoration:none;border-radius:4px;">'
        'Sign In Now</a></p>'
        '<p>For your security, choose a new username and password as soon as you sign in. '
        'Do not share these credentials with anyone.</p>'
        '<p>If you were not expecting this account, please contact your administrator.</p>',
    NULL,
    'Branch Account Ready',
    'Your account is ready. Check your email for login credentials.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    description              = VALUES(description),
    channels                 = VALUES(channels),
    channel_delivery_enabled = VALUES(channel_delivery_enabled),
    email_subject            = VALUES(email_subject),
    email_body_html          = VALUES(email_body_html),
    sms_body                 = VALUES(sms_body),
    in_app_title             = VALUES(in_app_title),
    in_app_body              = VALUES(in_app_body),
    is_active                = VALUES(is_active),
    entity_status            = VALUES(entity_status),
    updated_at               = NOW(6);
