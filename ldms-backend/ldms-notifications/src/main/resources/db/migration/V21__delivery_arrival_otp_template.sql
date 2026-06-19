-- Delivery arrival OTP notification templates.
-- Sent to the receiver/customer when the driver triggers arrival and the OTP is generated.
-- Supports SMS, WhatsApp and Email channels.

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
    'DELIVERY_ARRIVAL_OTP',
    'One-time passcode sent to the receiver to confirm delivery when the driver arrives at destination',
    JSON_ARRAY('SMS', 'WHATSAPP', 'EMAIL'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', TRUE, 'WHATSAPP', TRUE),
    'Your delivery OTP for {{tripNumber}}',
    '<p>Hello {{receiverName}},</p>'
        '<p>The driver has arrived at the delivery point for trip <strong>{{tripNumber}}</strong>.</p>'
        '<p>Your delivery confirmation code is:</p>'
        '<h2 style="letter-spacing:4px;">{{otp}}</h2>'
        '<p>This code expires in <strong>30 minutes</strong>. Do not share it with anyone other than the delivery driver.</p>'
        '<p>Shipment: {{shipmentNumber}}<br>'
        'Product: {{productName}}<br>'
        'From: {{fromWarehouseName}}<br>'
        'To: {{toWarehouseName}}</p>'
        '<p>If you were not expecting a delivery, please contact your supplier immediately.</p>',
    'Your Project LX delivery OTP for trip {{tripNumber}} is {{otp}}. It expires in 30 minutes. Share only with your delivery driver.',
    'Delivery OTP — {{tripNumber}}',
    'Your delivery confirmation code is {{otp}}. Valid for 30 minutes.',
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
