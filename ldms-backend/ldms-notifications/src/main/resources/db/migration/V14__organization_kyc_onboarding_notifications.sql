-- KYC lifecycle templates (submission + multi-stage approval) and onboarding CTA fixes.
-- Email mandatory; SMS/WhatsApp optional (disabled until enabled in admin).

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
    'ORG_KYC_SUBMITTED',
    'Notifies organisation and contact when KYC is submitted for review',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — KYC application submitted',
    '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width,initial-scale=1.0" /></head><body style="margin:0;padding:0;background:#edf2f7;font-family:Arial,Helvetica,sans-serif"><table width="100%" cellpadding="0" cellspacing="0" style="background:#edf2f7"><tr><td align="center" style="padding:40px 16px"><table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:18px;overflow:hidden"><tr><td style="background:linear-gradient(135deg,#0f1c45 0%,#1b2a5e 50%,#2c3d85 100%);padding:32px 48px 30px"><table cellpadding="0" cellspacing="0"><tr><td style="background:#3b82f6;border-radius:10px;padding:10px 16px;font-size:20px;font-weight:800;color:#ffffff">LX</td><td style="padding-left:16px;vertical-align:middle"><div style="font-size:19px;font-weight:700;color:#ffffff">Project LX</div><div style="font-size:11px;color:rgba(255,255,255,0.48);margin-top:3px">Logistics &amp; Distribution Platform</div></td></tr></table></td></tr><tr><td height="4" style="background:linear-gradient(90deg,#3b82f6,#818cf8,#f97316);font-size:0">&nbsp;</td></tr><tr><td style="padding:38px 48px 44px"><table cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td style="background:#eff6ff;border:1.5px solid #bfdbfe;border-radius:20px;padding:6px 16px"><span style="font-size:11px;font-weight:700;color:#1e40af">&#128203; APPLICATION SUBMITTED</span></td></tr></table><p style="margin:0 0 14px;font-size:27px;font-weight:700;color:#0f1c45">Hi {{contactName}},</p><p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8">{{statusDetail}}</p><table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td align="center"><table cellpadding="0" cellspacing="0"><tr><td style="background:linear-gradient(135deg,#1b2a5e,#2c3d85);border-radius:50px"><a href="{{nextStepsLink}}" style="display:inline-block;padding:16px 52px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none">Track onboarding progress &rarr;</a></td></tr></table></td></tr></table><p style="margin:0;font-size:13px;color:#94a3b8;text-align:center">Sign in: <span style="color:#3b82f6">{{signInLink}}</span></p></td></tr></table></td></tr></table></body></html>',
    NULL,
    'KYC submitted',
    'Your KYC application for {{organizationName}} was submitted.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'ORG_KYC_STAGE_APPROVED',
    'Notifies organisation and contact when an intermediate KYC approval stage completes',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — {{statusHeadline}}',
    '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width,initial-scale=1.0" /></head><body style="margin:0;padding:0;background:#edf2f7;font-family:Arial,Helvetica,sans-serif"><table width="100%" cellpadding="0" cellspacing="0" style="background:#edf2f7"><tr><td align="center" style="padding:40px 16px"><table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:18px;overflow:hidden"><tr><td style="background:linear-gradient(135deg,#0f1c45 0%,#1b2a5e 50%,#2c3d85 100%);padding:32px 48px 30px"><table cellpadding="0" cellspacing="0"><tr><td style="background:#3b82f6;border-radius:10px;padding:10px 16px;font-size:20px;font-weight:800;color:#ffffff">LX</td><td style="padding-left:16px;vertical-align:middle"><div style="font-size:19px;font-weight:700;color:#ffffff">Project LX</div></td></tr></table></td></tr><tr><td height="4" style="background:linear-gradient(90deg,#3b82f6,#818cf8,#f97316);font-size:0">&nbsp;</td></tr><tr><td style="padding:38px 48px 44px"><table cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td style="background:#ecfdf5;border:1.5px solid #a7f3d0;border-radius:20px;padding:6px 16px"><span style="font-size:11px;font-weight:700;color:#065f46">&#10003; {{statusHeadline}}</span></td></tr></table><p style="margin:0 0 14px;font-size:27px;font-weight:700;color:#0f1c45">Hi {{contactName}},</p><p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8">{{statusDetail}}</p><p style="margin:0 0 26px;font-size:14px;color:#64748b">Progress: <strong>{{stageLabel}}</strong> of <strong>{{totalStages}}</strong> &middot; Next: <strong>{{nextStageLabel}}</strong></p><table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td align="center"><table cellpadding="0" cellspacing="0"><tr><td style="background:linear-gradient(135deg,#1b2a5e,#2c3d85);border-radius:50px"><a href="{{nextStepsLink}}" style="display:inline-block;padding:16px 52px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none">View onboarding progress &rarr;</a></td></tr></table></td></tr></table></td></tr></table></td></tr></table></body></html>',
    NULL,
    'KYC stage approved',
    '{{statusHeadline}} for {{organizationName}}.',
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

-- Ensure all organisation KYC templates deliver email (SMS/WhatsApp off until enabled).
UPDATE notification_template
SET
    channels = JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    channel_delivery_enabled = JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    updated_at = NOW(6)
WHERE template_key IN (
    'ORGANIZATION_SIGNUP_RECEIVED',
    'ORGANIZATION_REGISTERED_BY_ADMIN',
    'ORG_KYC_STAGE1_APPROVED',
    'ORG_KYC_STAGE2_APPROVED',
    'ORG_KYC_REJECTED',
    'ORG_KYC_RESUBMISSION_ALLOWED',
    'ORG_KYC_SUBMITTED',
    'ORG_KYC_STAGE_APPROVED'
);

-- Registration received: CTA must open onboarding progress, not signup again.
UPDATE notification_template
SET
    email_body_html = REPLACE(
        email_body_html,
        'Continue application &rarr;',
        'Track onboarding progress &rarr;'
    ),
    updated_at = NOW(6)
WHERE template_key = 'ORGANIZATION_SIGNUP_RECEIVED'
  AND email_body_html LIKE '%Continue application%';

UPDATE notification_template
SET
    email_subject = '{{organizationName}} — we received your application',
    updated_at = NOW(6)
WHERE template_key = 'ORGANIZATION_SIGNUP_RECEIVED';

-- Stage 1 template: dynamic progress copy + onboarding CTA label.
UPDATE notification_template
SET
    email_subject = '{{organizationName}} — {{statusHeadline}}',
    email_body_html = REPLACE(
        REPLACE(email_body_html, 'View application &rarr;', 'View onboarding progress &rarr;'),
        'Stage 2 review',
        '{{nextStageLabel}}'
    ),
    updated_at = NOW(6)
WHERE template_key = 'ORG_KYC_STAGE1_APPROVED';

UPDATE notification_template
SET
    email_body_html = REPLACE(email_body_html, 'View application &rarr;', 'View onboarding progress &rarr;'),
    updated_at = NOW(6)
WHERE template_key IN ('ORG_KYC_REJECTED', 'ORG_KYC_RESUBMISSION_ALLOWED')
  AND email_body_html LIKE '%View application%';
