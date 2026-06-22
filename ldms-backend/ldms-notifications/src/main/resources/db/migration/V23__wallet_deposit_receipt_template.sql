-- Wallet top-up / credit receipt email (HTML receipt block; save email as PDF for records).

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
    'WALLET_DEPOSIT_RECEIPT',
    'Wallet credit receipt after deposit confirmation or admin credit',
    JSON_ARRAY('EMAIL'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — wallet receipt {{receiptNumber}}',
    '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/></head><body style="margin:0;padding:0;background:#edf2f7;font-family:Arial,Helvetica,sans-serif"><table width="100%" cellpadding="0" cellspacing="0" style="background:#edf2f7"><tr><td align="center" style="padding:32px 16px"><table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:18px;overflow:hidden"><tr><td style="background:linear-gradient(135deg,#0f1c45,#1b2a5e);padding:28px 40px"><div style="font-size:20px;font-weight:800;color:#fff">Project LX</div><div style="font-size:13px;color:#cbd5e1;margin-top:6px">Wallet payment receipt</div></td></tr><tr><td height="4" style="background:linear-gradient(90deg,#ea580c,#f97316)">&nbsp;</td></tr><tr><td style="padding:32px 40px 36px"><p style="margin:0 0 12px;font-size:22px;font-weight:700;color:#0f172a">Payment received</p><p style="margin:0 0 20px;font-size:15px;color:#475569;line-height:1.7">Your platform wallet for <strong>{{organizationName}}</strong> was credited <strong>{{amountFormatted}}</strong>. New balance: <strong>{{balanceAfterFormatted}}</strong>.</p><p style="margin:0 0 16px;font-size:13px;color:#64748b">Receipt <strong>{{receiptNumber}}</strong> — you can save this email as a PDF for your records.</p><div style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">{{receiptHtml}}</div></td></tr></table></td></tr></table></body></html>',
    NULL,
    'Wallet credited',
    'Your LX wallet was credited {{amountFormatted}}.',
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
