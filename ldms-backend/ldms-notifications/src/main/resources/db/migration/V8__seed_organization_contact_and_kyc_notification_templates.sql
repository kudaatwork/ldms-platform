-- Contact-person verification and organisation KYC stage notifications (ldms-organization-management).
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
    'ORG_CONTACT_PERSON_VERIFICATION',
    'Email verification for the organisation contact person to activate their LDMS user account',
    JSON_ARRAY('EMAIL'),
    'Verify your email to join {{organizationName}} on Project LX',
    '<p>Hello {{firstName}},</p><p>You have been listed as the contact person for <strong>{{organizationName}}</strong> on Project LX LDMS.</p><p>Please verify your email to activate your user account and access your organisation portal: <a href="{{verificationLink}}">Verify email</a>.</p><p>After verification you can sign in at <a href="{{signInLink}}">{{signInLink}}</a>. Use <strong>Forgot password</strong> on first sign-in to set your password.</p><p>If you did not expect this message, you can ignore it.</p>',
    NULL,
    NULL,
    NULL,
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'ORG_KYC_STAGE1_APPROVED',
    'Notifies organisation and contact when KYC stage 1 is approved (application advances to stage 2 review)',
    JSON_ARRAY('EMAIL'),
    '{{organizationName}} — KYC stage 1 approved',
    '<p>Hello {{contactName}},</p><p>Good news: the KYC review for <strong>{{organizationName}}</strong> has completed <strong>Stage 1</strong> successfully.</p><p>Your application is now in <strong>Stage 2 review</strong>. We will notify you when the next decision is made.</p><p>Track progress or update your profile: <a href="{{nextStepsLink}}">{{nextStepsLink}}</a>.</p><p>Sign in: <a href="{{signInLink}}">{{signInLink}}</a>.</p>',
    NULL,
    NULL,
    NULL,
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'ORG_KYC_STAGE2_APPROVED',
    'Notifies organisation and contact when KYC is fully approved and the organisation is verified',
    JSON_ARRAY('EMAIL'),
    '{{organizationName}} is verified on Project LX',
    '<p>Hello {{contactName}},</p><p><strong>{{organizationName}}</strong> has completed KYC and is now <strong>verified</strong> on Project LX LDMS.</p><p>You can sign in and use the platform: <a href="{{signInLink}}">{{signInLink}}</a>.</p><p>Thank you for completing the verification process.</p>',
    NULL,
    NULL,
    NULL,
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
