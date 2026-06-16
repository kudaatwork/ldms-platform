-- Logistics lifecycle notifications: SHIPMENT_ALLOCATED, TRIP_STARTED, TRIP_COMPLETED
-- EMAIL channel enabled; SMS and WhatsApp prepared for future activation.
-- Template variables: contactName, organizationName, shipmentNumber, tripNumber,
--   fromWarehouse, toWarehouse, productName, quantity, driverName, performedBy,
--   signInLink, recipientRole, lifecycleMessage

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
    'SHIPMENT_ALLOCATED',
    'Notifies organisation contacts, fleet managers, and the assigned driver when a shipment is allocated',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — Shipment {{shipmentNumber}} allocated',
    '<p>Hello {{contactName}},</p><p>{{lifecycleMessage}}</p><p><strong>Shipment:</strong> {{shipmentNumber}}<br/><strong>From:</strong> {{fromWarehouse}}<br/><strong>To:</strong> {{toWarehouse}}<br/><strong>Product:</strong> {{productName}}<br/><strong>Quantity:</strong> {{quantity}}<br/><strong>Performed by:</strong> {{performedBy}}</p><p>Sign in to view details: <a href="{{signInLink}}">{{signInLink}}</a></p>',
    NULL,
    'Shipment Allocated',
    'Shipment {{shipmentNumber}} has been allocated for dispatch from {{fromWarehouse}} to {{toWarehouse}}.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'TRIP_STARTED',
    'Notifies organisation contacts, fleet managers, and the driver when a trip starts',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — Trip {{tripNumber}} started',
    '<p>Hello {{contactName}},</p><p>{{lifecycleMessage}}</p><p><strong>Trip:</strong> {{tripNumber}}<br/><strong>Shipment:</strong> {{shipmentNumber}}<br/><strong>From:</strong> {{fromWarehouse}}<br/><strong>To:</strong> {{toWarehouse}}<br/><strong>Product:</strong> {{productName}}<br/><strong>Driver:</strong> {{driverName}}<br/><strong>Started by:</strong> {{performedBy}}</p><p>Sign in to track the trip: <a href="{{signInLink}}">{{signInLink}}</a></p>',
    NULL,
    'Trip Started',
    'Trip {{tripNumber}} has started from {{fromWarehouse}} to {{toWarehouse}}.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
),
(
    'TRIP_COMPLETED',
    'Notifies organisation contacts, fleet managers, and the driver when a trip is completed (delivery confirmed)',
    JSON_ARRAY('EMAIL', 'SMS', 'WHATSAPP'),
    JSON_OBJECT('EMAIL', TRUE, 'SMS', FALSE, 'WHATSAPP', FALSE),
    '{{organizationName}} — Trip {{tripNumber}} completed',
    '<p>Hello {{contactName}},</p><p>{{lifecycleMessage}}</p><p><strong>Trip:</strong> {{tripNumber}}<br/><strong>Shipment:</strong> {{shipmentNumber}}<br/><strong>From:</strong> {{fromWarehouse}}<br/><strong>To:</strong> {{toWarehouse}}<br/><strong>Product:</strong> {{productName}}<br/><strong>Driver:</strong> {{driverName}}<br/><strong>Confirmed by:</strong> {{performedBy}}</p><p>Sign in to view the delivery summary: <a href="{{signInLink}}">{{signInLink}}</a></p>',
    NULL,
    'Trip Completed',
    'Trip {{tripNumber}} has been completed. Delivery to {{toWarehouse}} was confirmed.',
    1,
    'ACTIVE',
    NOW(6),
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    description             = VALUES(description),
    channels                = VALUES(channels),
    channel_delivery_enabled = VALUES(channel_delivery_enabled),
    email_subject           = VALUES(email_subject),
    email_body_html         = VALUES(email_body_html),
    sms_body                = VALUES(sms_body),
    in_app_title            = VALUES(in_app_title),
    in_app_body             = VALUES(in_app_body),
    is_active               = VALUES(is_active),
    updated_at              = NOW(6);
