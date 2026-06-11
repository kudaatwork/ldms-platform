-- ========================================================================
-- Flyway Migration V2: Purchase Order Tables (CORRECTED)
-- ========================================================================
-- Purpose: Purchase order management matching PurchaseOrder.java entity
-- Reference: PurchaseOrder.java and PurchaseOrderLine.java
-- ========================================================================

-- ========================================================================
-- Table: purchase_order
-- Entity: PurchaseOrder.java
-- ========================================================================
CREATE TABLE purchase_order (
                                id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identification
                                purchase_order_number VARCHAR(50) NOT NULL UNIQUE,
                                external_id VARCHAR(100),

    -- PR Reference (if auto-created from requisition)
                                purchase_requisition_id BIGINT,

    -- Parties
                                organization_id BIGINT NOT NULL COMMENT 'Your company making the purchase',
                                supplier_id BIGINT NOT NULL COMMENT 'FK to Org Service',
                                buyer_contact VARCHAR(200) NOT NULL,
                                supplier_contact VARCHAR(200) NOT NULL,

    -- Financial Terms
                                currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                                payment_term VARCHAR(50) COMMENT 'ENUM: PaymentTerm from shared library',
                                payment_due_date DATE NOT NULL,
                                early_payment_discount_pct DECIMAL(5, 2),
                                early_payment_discount_until DATE,
                                prepayment_required BOOLEAN DEFAULT FALSE,
                                prepayment_percent DECIMAL(5, 2),

    -- Amounts
                                subtotal DECIMAL(19, 4) DEFAULT 0.0000,
                                tax_rate DECIMAL(5, 2),
                                tax_amount DECIMAL(19, 4) DEFAULT 0.0000,
                                total_amount DECIMAL(19, 4) DEFAULT 0.0000,

    -- Shipping & Logistics
                                ship_from_location_id BIGINT NOT NULL COMMENT 'FK to Location Service',
                                ship_to_location_id BIGINT NOT NULL COMMENT 'FK to Location Service',
                                receiving_warehouse_id BIGINT NOT NULL COMMENT 'FK to warehouse_location table',
                                freight_terms VARCHAR(30) COMMENT 'ENUM: FOB, CIF, EXW, DDP',
                                ship_mode VARCHAR(30) NOT NULL COMMENT 'ENUM: ROAD, AIR, SEA, RAIL',
                                shipping_instructions TEXT,

    -- Import/Export (cross-border)
                                is_import BOOLEAN DEFAULT FALSE,
                                customs_declaration_number VARCHAR(100),
                                port_of_entry VARCHAR(100),

    -- Status & Workflow
                                status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'ENUM: PurchaseOrderStatus',
                                order_date DATE NOT NULL,
                                expected_date DATE,
                                received_date DATETIME(6),

    -- Approval
                                approved_by_user_id BIGINT,
                                approved_at DATETIME(6),
                                approval_notes TEXT,

    -- Audit fields
                                created_by_user_id BIGINT,
                                updated_by_user_id BIGINT,
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Notes
                                notes TEXT,

                                PRIMARY KEY (id),
                                CONSTRAINT fk_po_receiving_warehouse FOREIGN KEY (receiving_warehouse_id)
                                    REFERENCES warehouse_location(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Purchase orders';

CREATE UNIQUE INDEX ux_purchase_order_purchase_order_number ON purchase_order(purchase_order_number);
CREATE INDEX idx_po_supplier_status ON purchase_order(supplier_id, status);
CREATE INDEX idx_po_order_date ON purchase_order(order_date);
CREATE INDEX idx_po_organization ON purchase_order(organization_id);
CREATE INDEX idx_po_warehouse ON purchase_order(receiving_warehouse_id);
CREATE INDEX idx_po_status ON purchase_order(status);
CREATE INDEX idx_po_pr_id ON purchase_order(purchase_requisition_id);

-- ========================================================================
-- Table: purchase_order_line
-- Entity: PurchaseOrderLine.java
-- ========================================================================
CREATE TABLE purchase_order_line (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     purchase_order_id BIGINT NOT NULL,
                                     line_number INT NOT NULL,
                                     product_id BIGINT NOT NULL,

    -- Quantities
                                     quantity DECIMAL(19, 4) NOT NULL,
                                     received_quantity DECIMAL(19, 4) DEFAULT 0.0000,

    -- Pricing
                                     unit_price DECIMAL(19, 4) NOT NULL,
                                     total_price DECIMAL(19, 4) NOT NULL,

    -- Audit fields
                                     created_by_user_id BIGINT,
                                     updated_by_user_id BIGINT,
                                     created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                     updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                     entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                     PRIMARY KEY (id),
                                     CONSTRAINT fk_po_line_po FOREIGN KEY (purchase_order_id)
                                         REFERENCES purchase_order(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_po_line_product FOREIGN KEY (product_id)
                                         REFERENCES product(id) ON DELETE RESTRICT,
                                     CONSTRAINT ux_po_line_number UNIQUE (purchase_order_id, line_number)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Purchase order line items';

CREATE INDEX idx_po_line_po ON purchase_order_line(purchase_order_id);
CREATE INDEX idx_po_line_product ON purchase_order_line(product_id);

-- ========================================================================
-- END OF V2 MIGRATION
-- ========================================================================