-- Organisation inbox verification (supplier-registered customers/transporters).

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
    'ORG_EMAIL_VERIFICATION',
    'Organisation email verification link after supplier registration',
    JSON_ARRAY('EMAIL'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — verify your organisation email',
    '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width,initial-scale=1.0" /></head><body style="margin:0;padding:0;background:#edf2f7;font-family:Arial,Helvetica,sans-serif"><table width="100%" cellpadding="0" cellspacing="0" style="background:#edf2f7"><tr><td align="center" style="padding:40px 16px"><table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:18px;overflow:hidden"><tr><td style="background:linear-gradient(135deg,#0f1c45 0%,#1b2a5e 50%,#2c3d85 100%);padding:32px 48px 30px"><table cellpadding="0" cellspacing="0"><tr><td style="background:#3b82f6;border-radius:10px;padding:10px 16px;font-size:20px;font-weight:800;color:#ffffff">LX</td><td style="padding-left:16px;vertical-align:middle"><div style="font-size:19px;font-weight:700;color:#ffffff">Project LX</div></td></tr></table></td></tr><tr><td height="4" style="background:linear-gradient(90deg,#3b82f6,#818cf8,#f97316);font-size:0">&nbsp;</td></tr><tr><td style="padding:38px 48px 44px"><table cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td style="background:#eff6ff;border:1.5px solid #bfdbfe;border-radius:20px;padding:6px 16px"><span style="font-size:11px;font-weight:700;color:#1e40af">&#128231; VERIFY ORGANISATION EMAIL</span></td></tr></table><p style="margin:0 0 14px;font-size:27px;font-weight:700;color:#0f1c45">Hi {{contactName}},</p><p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8">{{statusDetail}}</p><table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td align="center"><table cellpadding="0" cellspacing="0"><tr><td style="background:linear-gradient(135deg,#1b2a5e,#2c3d85);border-radius:50px"><a href="{{verificationLink}}" style="display:inline-block;padding:16px 52px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none">Verify organisation email &rarr;</a></td></tr></table></td></tr></table><p style="margin:0;font-size:14px;color:#64748b;line-height:1.7">The primary contact person has been emailed separate sign-in details. If you did not expect this registration, you can ignore this message.</p></td></tr></table></td></tr></table></body></html>',
    NULL,
    'Verify organisation email',
    'Confirm the organisation email for {{organizationName}} on Project LX.',
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
    in_app_title = VALUES(in_app_title),
    in_app_body = VALUES(in_app_body),
    is_active = VALUES(is_active),
    entity_status = VALUES(entity_status),
    updated_at = NOW(6);

UPDATE notification_template
SET channel_delivery_enabled = JSON_SET(COALESCE(channel_delivery_enabled, JSON_OBJECT()), '$.SMS', FALSE, '$.WHATSAPP', FALSE)
WHERE template_key = 'ORG_EMAIL_VERIFICATION';
