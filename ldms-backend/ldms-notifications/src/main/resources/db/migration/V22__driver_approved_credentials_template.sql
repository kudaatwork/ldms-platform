-- Driver approved credentials notification template.
-- Sent to a driver when their platform user account is provisioned (signup request approved or direct creation).
-- Supports EMAIL and SMS channels.

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
    'DRIVER_APPROVED_CREDENTIALS',
    'Temporary login credentials sent to a newly onboarded fleet driver',
    JSON_ARRAY('EMAIL', 'SMS'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', TRUE, 'WHATSAPP', FALSE),
    'Welcome to Project LX — Your Driver Login Credentials',
    '<p>Hello {{firstName}},</p>'
        '<p>Your driver account on the Project LX platform has been approved and created.</p>'
        '<p>Use the following temporary credentials to sign in and set up your permanent password:</p>'
        '<table style="border-collapse:collapse;margin:12px 0">'
        '<tr><td style="padding:4px 12px 4px 0;font-weight:bold;">Username:</td>'
        '<td style="padding:4px 0;">{{temporaryUsername}}</td></tr>'
        '<tr><td style="padding:4px 12px 4px 0;font-weight:bold;">Password:</td>'
        '<td style="padding:4px 0;">{{temporaryPassword}}</td></tr>'
        '</table>'
        '<p><a href="{{signInLink}}" '
        'style="display:inline-block;padding:10px 24px;background:#1565C0;color:#fff;text-decoration:none;border-radius:4px;">'
        'Sign In Now</a></p>'
        '<p>You will be prompted to choose a new password on first login. '
        'Do not share these credentials with anyone.</p>'
        '<p>If you did not request this account, please contact support immediately.</p>',
    'Welcome to Project LX! Your driver username is {{temporaryUsername}} and temporary password is {{temporaryPassword}}. Sign in at {{signInLink}} and change your password.',
    'Driver Account Approved',
    'Your driver account is ready. Check your email for login credentials.',
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
