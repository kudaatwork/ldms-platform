-- Platform prepaid wallet, subscription packages, per-action charges, and usage ledger.

CREATE TABLE platform_action_charge (
    id BIGINT NOT NULL AUTO_INCREMENT,
    action_code VARCHAR(80) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    charge_cents BIGINT NOT NULL DEFAULT 0,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    active TINYINT(1) NOT NULL DEFAULT 1,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT ux_platform_action_charge_code UNIQUE (action_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subscription_package (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    monthly_price_cents BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    sort_order INT NOT NULL DEFAULT 0,
    featured TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT ux_subscription_package_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organization_billing_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    organization_name VARCHAR(200) NOT NULL,
    billing_mode VARCHAR(50) NOT NULL DEFAULT 'PREPAID_WALLET',
    subscription_package_id BIGINT,
    subscription_started_at DATETIME(6),
    subscription_renews_at DATETIME(6),
    low_balance_threshold_cents BIGINT NOT NULL DEFAULT 500,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT ux_organization_billing_setting_org UNIQUE (organization_id),
    CONSTRAINT fk_org_billing_subscription_package
        FOREIGN KEY (subscription_package_id) REFERENCES subscription_package (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE platform_wallet (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    organization_name VARCHAR(200) NOT NULL,
    balance_cents BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT ux_platform_wallet_org UNIQUE (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_deposit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    reference_number VARCHAR(100),
    notes VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    proof_document_id BIGINT,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id),
    INDEX idx_wallet_deposit_org_status (organization_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount_cents BIGINT NOT NULL,
    balance_after_cents BIGINT NOT NULL,
    action_code VARCHAR(80),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    trip_id BIGINT,
    season_id BIGINT,
    description VARCHAR(500),
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id),
    INDEX idx_wallet_tx_org_created (organization_id, created_at),
    INDEX idx_wallet_tx_trip (trip_id),
    INDEX idx_wallet_tx_season (season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usage_charge_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    billing_mode VARCHAR(50) NOT NULL,
    action_code VARCHAR(80) NOT NULL,
    action_display_name VARCHAR(200),
    charge_cents BIGINT NOT NULL,
    deducted TINYINT(1) NOT NULL DEFAULT 0,
    trip_id BIGINT,
    season_id BIGINT,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    service_name VARCHAR(100),
    trace_id VARCHAR(100),
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id),
    INDEX idx_usage_charge_org_created (organization_id, created_at),
    INDEX idx_usage_charge_trip (organization_id, trip_id),
    INDEX idx_usage_charge_season (organization_id, season_id),
    INDEX idx_usage_charge_action (organization_id, action_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default per-action charges (cents). Admin portal can adjust these globally.
INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, active, entity_status, created_at, created_by)
VALUES
    ('NOTIFICATION_EMAIL', 'Email notification', 'Outbound email via notifications service', 2, 'NOTIFICATIONS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('NOTIFICATION_SMS', 'SMS notification', 'Outbound SMS message', 8, 'NOTIFICATIONS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('NOTIFICATION_PUSH', 'Push notification', 'Mobile push notification', 1, 'NOTIFICATIONS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('TRIP_CREATE', 'Trip created', 'New trip record created', 15, 'TRIPS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('TRIP_TRACK', 'Trip tracking ping', 'GPS / status tracking update for a trip', 3, 'TRIPS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('TRIP_COMPLETE', 'Trip completed', 'Trip marked complete with audit trail', 10, 'TRIPS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('DOCUMENT_UPLOAD', 'Document upload', 'File stored in document service', 5, 'DOCUMENTS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('DOCUMENT_SHARE', 'Document shared', 'Document shared with another party', 4, 'DOCUMENTS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('INVOICE_GENERATE', 'Invoice generated', 'Invoice or billing document generated', 12, 'BILLING', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('AUDIT_LOG_WRITE', 'Audit log entry', 'Platform audit trail write', 1, 'PLATFORM', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('GPS_PING', 'GPS location ping', 'IoT / mobile GPS telemetry point', 2, 'IOT', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ORDER_CREATE', 'Order created', 'Purchase or sales order created', 8, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('SHIPMENT_UPDATE', 'Shipment update', 'Shipment status change event', 6, 'LOGISTICS', 1, 'ACTIVE', NOW(6), 'SYSTEM');

-- Default monthly subscription packages (premium alternative to prepaid wallet).
INSERT INTO subscription_package (code, name, description, monthly_price_cents, currency_code, sort_order, featured, active, entity_status, created_at, created_by)
VALUES
    ('STARTER', 'Starter', 'Essential platform access with standard usage tracking. Best for small teams.', 9900, 'USD', 10, 0, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('GROWTH', 'Growth', 'Higher volume corridors with priority support. Usage still tracked for reporting.', 24900, 'USD', 20, 1, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ENTERPRISE', 'Enterprise', 'Unlimited tracked actions with dedicated success manager.', 49900, 'USD', 30, 0, 1, 'ACTIVE', NOW(6), 'SYSTEM');
