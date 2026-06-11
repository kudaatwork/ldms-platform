-- Procurement workflow: multi-stage approvals (default 1, max 3), supplier quotes, dual PO approval

CREATE TABLE platform_procurement_policy (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    default_required_approval_stages INT NOT NULL DEFAULT 1,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(150) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO platform_procurement_policy (
    default_required_approval_stages,
    entity_status,
    created_at,
    created_by
) VALUES (
    1,
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    'SYSTEM'
);

CREATE TABLE organization_procurement_setting (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    required_approval_stages INT NULL COMMENT 'Null inherits platform default (1-3)',
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(150) NULL,
    UNIQUE KEY ux_org_procurement_setting_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE procurement_approval_review (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL COMMENT 'REQUISITION_INTERNAL, PO_CUSTOMER, PO_SUPPLIER, SALES_ORDER',
    document_id BIGINT NOT NULL,
    stage_number INT NOT NULL,
    decision VARCHAR(20) NOT NULL COMMENT 'APPROVED or REJECTED',
    reviewed_by_user_id BIGINT NOT NULL,
    reviewed_by_username VARCHAR(150) NOT NULL,
    reviewed_at DATETIME(6) NOT NULL,
    notes TEXT NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(150) NULL,
    KEY idx_procurement_review_doc (document_type, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_quote (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    quote_number VARCHAR(50) NOT NULL,
    purchase_requisition_id BIGINT NOT NULL,
    supplier_organization_id BIGINT NOT NULL,
    customer_organization_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    quote_source VARCHAR(30) NOT NULL DEFAULT 'SYSTEM_GENERATED' COMMENT 'SYSTEM_GENERATED or EXTERNAL_UPLOAD',
    external_document_id BIGINT NULL COMMENT 'File storage reference when uploaded',
    currency VARCHAR(3) NOT NULL,
    subtotal DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    tax_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    payment_term VARCHAR(50) NULL,
    delivery_terms TEXT NULL,
    validity_until DATE NULL,
    notes TEXT NULL,
    submitted_at DATETIME(6) NULL,
    submitted_by_user_id BIGINT NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(150) NULL,
    UNIQUE KEY ux_supplier_quote_number (quote_number),
    KEY idx_supplier_quote_pr (purchase_requisition_id),
    KEY idx_supplier_quote_supplier (supplier_organization_id, status),
    CONSTRAINT fk_supplier_quote_pr FOREIGN KEY (purchase_requisition_id)
        REFERENCES purchase_requisition(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_quote_line (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    supplier_quote_id BIGINT NOT NULL,
    purchase_requisition_line_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    product_id BIGINT NOT NULL,
    quoted_quantity DECIMAL(19, 4) NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    line_total DECIMAL(19, 4) NOT NULL,
    lead_time_days INT NULL,
    notes TEXT NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(150) NULL,
    KEY idx_supplier_quote_line_quote (supplier_quote_id),
    CONSTRAINT fk_supplier_quote_line_quote FOREIGN KEY (supplier_quote_id)
        REFERENCES supplier_quote(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE purchase_requisition
    ADD COLUMN current_approval_stage INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN required_approval_stages INT NULL AFTER current_approval_stage,
    ADD COLUMN published_to_supplier_at DATETIME(6) NULL AFTER approval_notes,
    ADD COLUMN supplier_quote_id BIGINT NULL AFTER published_to_supplier_at,
    ADD COLUMN acknowledged_at DATETIME(6) NULL AFTER supplier_quote_id,
    ADD COLUMN acknowledged_by_user_id BIGINT NULL AFTER acknowledged_at;

CREATE INDEX idx_pr_supplier_visibility ON purchase_requisition(preferred_supplier_id, status);
CREATE INDEX idx_pr_org_status ON purchase_requisition(organization_id, status);

ALTER TABLE purchase_order
    ADD COLUMN current_customer_approval_stage INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN current_supplier_approval_stage INT NOT NULL DEFAULT 0 AFTER current_customer_approval_stage,
    ADD COLUMN required_approval_stages INT NULL AFTER current_supplier_approval_stage,
    ADD COLUMN customer_approval_complete BOOLEAN NOT NULL DEFAULT FALSE AFTER required_approval_stages,
    ADD COLUMN supplier_approval_complete BOOLEAN NOT NULL DEFAULT FALSE AFTER customer_approval_complete,
    ADD COLUMN customer_approved_at DATETIME(6) NULL AFTER supplier_approval_complete,
    ADD COLUMN supplier_approved_at DATETIME(6) NULL AFTER customer_approved_at,
    ADD COLUMN payment_confirmed BOOLEAN NOT NULL DEFAULT FALSE AFTER supplier_approved_at,
    ADD COLUMN payment_confirmed_at DATETIME(6) NULL AFTER payment_confirmed;

ALTER TABLE sales_order
    ADD COLUMN current_approval_stage INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN required_approval_stages INT NULL AFTER current_approval_stage,
    ADD COLUMN approval_complete BOOLEAN NOT NULL DEFAULT FALSE AFTER required_approval_stages,
    ADD COLUMN approved_at DATETIME(6) NULL AFTER approval_complete,
    ADD COLUMN approved_by_user_id BIGINT NULL AFTER approved_at,
    ADD COLUMN inventory_reserved_at_shipment BOOLEAN NOT NULL DEFAULT TRUE AFTER approved_by_user_id;
