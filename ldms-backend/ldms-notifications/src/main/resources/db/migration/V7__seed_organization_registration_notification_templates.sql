-- Templates used by ldms-organization-management on organisation register.
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
    'ORGANIZATION_REGISTERED_BY_ADMIN',
    'Confirmation when an administrator registers an organisation on the admin portal',
    JSON_ARRAY('EMAIL'),
    'Your organisation is registered on Project LX',
    '<p>Hello {{contactName}},</p><p>Organisation <strong>{{organizationName}}</strong> has been registered on Project LX LDMS by an administrator.</p><p>You can sign in at <a href="{{signInLink}}">{{signInLink}}</a> when your portal access is ready.</p>',
    NULL,
    NULL,
    NULL,
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'ORGANIZATION_SIGNUP_RECEIVED',
    'Acknowledgement when an organisation applies via the platform signup flow',
    JSON_ARRAY('EMAIL'),
    'We received your organisation registration',
    '<p>Hello {{contactName}},</p><p>Thank you for registering <strong>{{organizationName}}</strong> on Project LX.</p><p>Complete your application and submit KYC when ready: <a href="{{nextStepsLink}}">{{nextStepsLink}}</a>.</p><p>Sign in later at <a href="{{signInLink}}">{{signInLink}}</a>.</p>',
    NULL,
    NULL,
    NULL,
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
);
