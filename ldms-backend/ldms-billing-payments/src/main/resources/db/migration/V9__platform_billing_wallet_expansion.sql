-- Wallet deposits: payment channel metadata. Transactions: receipt tracking.

ALTER TABLE wallet_deposit
    ADD COLUMN gateway_provider VARCHAR(50) NULL AFTER proof_document_id,
    ADD COLUMN payment_method VARCHAR(50) NULL AFTER gateway_provider;

ALTER TABLE wallet_transaction
    ADD COLUMN receipt_number VARCHAR(40) NULL AFTER description,
    ADD COLUMN receipt_document_id BIGINT NULL AFTER receipt_number;

-- Expanded per-action pricing (small usage fees in USD cents).
INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, active, entity_status, created_at, created_by)
VALUES
    ('FLEET_DRIVER_HIRE', 'Hire freelance driver', 'Add a driver from the marketplace to your roster', 12, 'FLEET', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('FLEET_VEHICLE_REGISTER', 'Register fleet vehicle', 'New fleet asset registration', 8, 'FLEET', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('FLEET_COMPLIANCE_UPLOAD', 'Fleet compliance document', 'Upload fleet or driver compliance record', 4, 'FLEET', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('INVENTORY_GRV_CREATE', 'Goods received (GRV)', 'Record goods received at destination', 10, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('INVENTORY_TRANSFER', 'Stock transfer', 'Inter-warehouse stock transfer', 7, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('INVENTORY_STOCK_RESERVE', 'Stock reservation', 'Reserve stock against a purchase order', 5, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('SHIPMENT_DISPATCH', 'Shipment dispatch', 'Create or release a shipment dispatch', 15, 'LOGISTICS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('TRIP_ASSIGN_DRIVER', 'Assign driver to trip', 'Driver assignment to an active trip', 8, 'TRIPS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('FUEL_FUND_REQUEST', 'Fuel fund request', 'Driver fuel or operational fund request', 6, 'LOGISTICS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ROADSIDE_INCIDENT', 'Roadside incident log', 'Mechanic or roadside support incident', 5, 'LOGISTICS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('WHATSAPP_COMMAND', 'WhatsApp bot command', 'Inbound WhatsApp command processed', 3, 'NOTIFICATIONS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('API_INTEGRATION_CALL', 'Integration API call', 'External integration API invocation', 2, 'PLATFORM', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('REPORT_EXPORT', 'Report export', 'Export analytics or usage report', 9, 'PLATFORM', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ORG_CUSTOMER_REGISTER', 'Register customer org', 'Supplier registers a customer organisation', 10, 'PLATFORM', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('LIVE_MAP_SESSION', 'Live map session', 'Ops live trip map refresh cycle', 4, 'TRIPS', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';
