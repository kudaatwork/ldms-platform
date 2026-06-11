-- ========================================================================
-- Flyway Migration V3: Sales Order Tables (CORRECTED)
-- ========================================================================
-- Purpose: Sales order management matching SalesOrder.java entity
-- Reference: SalesOrder.java and SalesOrderLine.java
-- ========================================================================

-- ========================================================================
-- Table: sales_order
-- Entity: SalesOrder.java (extends BaseEntity)
-- ========================================================================
CREATE TABLE sales_order (
                             id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identification
                             sales_order_number VARCHAR(50) NOT NULL,

    -- PO → SO Linkage Fields
                             purchase_order_id BIGINT NOT NULL COMMENT 'Reference to originating Purchase Order',
                             purchase_order_number VARCHAR(50) NOT NULL COMMENT 'Copy of PO number for traceability',

    -- Organization & Fulfillment
                             supplier_organization_id BIGINT NOT NULL COMMENT 'Supplier fulfilling this order',
                             fulfillment_warehouse_id BIGINT COMMENT 'Warehouse from which goods will be fulfilled',

    -- Confirmation tracking
                             confirmed_at DATETIME(6) COMMENT 'When supplier confirmed they can fulfill',
                             confirmed_by_user_id BIGINT COMMENT 'User who confirmed the SO on supplier side',

    -- Customer & Dates
                             customer_id BIGINT NOT NULL COMMENT 'FK to Org Service',
                             order_date DATE NOT NULL,
                             expected_delivery_date DATE,
                             delivered_date DATETIME(6),

    -- Status & Payment
                             status VARCHAR(30) NOT NULL COMMENT 'ENUM: SalesOrderStatus',
                             total_amount DECIMAL(19, 4),
                             payment_term VARCHAR(50) COMMENT 'ENUM: PaymentTerm',

    -- Shipment Reference
                             shipment_id BIGINT COMMENT 'Reference to Shipment Management Service',
                             shipment_created_at DATETIME(6),

    -- Notes
                             notes TEXT,

    -- Audit fields (from BaseEntity)
                             created_by_user_id BIGINT,
                             updated_by_user_id BIGINT,
                             created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                             entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                             PRIMARY KEY (id),
                             CONSTRAINT fk_so_fulfillment_warehouse FOREIGN KEY (fulfillment_warehouse_id)
                                 REFERENCES warehouse_location(id) ON DELETE SET NULL,
                             CONSTRAINT fk_so_purchase_order FOREIGN KEY (purchase_order_id)
                                 REFERENCES purchase_order(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Sales orders (supplier view of customer PO)';

CREATE UNIQUE INDEX ux_sales_order_sales_order_number ON sales_order(sales_order_number);
CREATE INDEX idx_sales_order_purchase_order_id ON sales_order(purchase_order_id);
CREATE INDEX idx_sales_order_supplier_org_id ON sales_order(supplier_organization_id);
CREATE INDEX idx_sales_order_customer ON sales_order(customer_id);
CREATE INDEX idx_sales_order_status ON sales_order(status);
CREATE INDEX idx_sales_order_order_date ON sales_order(order_date);

-- ========================================================================
-- Table: sales_order_line
-- Entity: SalesOrderLine.java (extends BaseEntity)
-- ========================================================================
CREATE TABLE sales_order_line (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  sales_order_id BIGINT NOT NULL,
                                  product_id BIGINT NOT NULL,

    -- Quantities
                                  quantity DECIMAL(19, 4) NOT NULL,
                                  fulfilled_quantity DECIMAL(19, 4) DEFAULT 0.0000,

    -- Pricing
                                  unit_price DECIMAL(19, 4) NOT NULL,
                                  total_price DECIMAL(19, 4) NOT NULL,

    -- UnitOfMeasure is an ENUM stored as VARCHAR
                                  unit_of_measure VARCHAR(50) COMMENT 'ENUM: EACH, BOX, PACK, KILOGRAM, etc.',

    -- Audit fields (from BaseEntity)
                                  created_by_user_id BIGINT,
                                  updated_by_user_id BIGINT,
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_so_line_sales_order FOREIGN KEY (sales_order_id)
                                      REFERENCES sales_order(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_so_line_product FOREIGN KEY (product_id)
                                      REFERENCES product(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Sales order line items';

CREATE INDEX idx_so_line_sales_order ON sales_order_line(sales_order_id);
CREATE INDEX idx_so_line_product ON sales_order_line(product_id);

-- ========================================================================
-- END OF V3 MIGRATION
-- ========================================================================