-- ========================================================================
-- Flyway Migration V4: Inventory Tracking Tables (CORRECTED)
-- ========================================================================
-- Purpose: Real-time inventory levels and reservations
-- Reference: InventoryItem.java and InventoryReservation.java
-- ========================================================================

-- ========================================================================
-- Table: inventory_item
-- Entity: InventoryItem.java
-- ========================================================================
CREATE TABLE inventory_item (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                product_id BIGINT NOT NULL,
                                supplier_id BIGINT NOT NULL COMMENT 'FK to Org Service',
                                warehouse_location_id BIGINT NOT NULL,

    -- Quantity fields (using DECIMAL for precision)
                                quantity DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                                current_stock DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
                                reserved_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,

    -- Costing fields (Weighted Average Cost)
                                total_cost DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
                                average_cost DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
                                last_purchase_cost DECIMAL(19, 4) DEFAULT 0.0000,
                                unit_cost DECIMAL(19, 4) DEFAULT 0.0000,

    -- Safety stock levels
                                reorder_level DECIMAL(19, 4),
                                reorder_quantity DECIMAL(19, 4),

    -- Last transaction tracking
                                last_transaction_date DATETIME(6),
                                last_purchase_date DATETIME(6),
                                last_sale_date DATETIME(6),

    -- Stock status
                                is_low_stock BOOLEAN DEFAULT FALSE,
                                is_out_of_stock BOOLEAN DEFAULT FALSE,

    -- Audit fields
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                PRIMARY KEY (id),
                                CONSTRAINT fk_inventory_item_product FOREIGN KEY (product_id)
                                    REFERENCES product(id) ON DELETE RESTRICT,
                                CONSTRAINT fk_inventory_item_warehouse FOREIGN KEY (warehouse_location_id)
                                    REFERENCES warehouse_location(id) ON DELETE RESTRICT,
                                CONSTRAINT uk_inventory_item_product_wh UNIQUE (product_id, warehouse_location_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Real-time inventory levels per product per warehouse';

CREATE INDEX idx_inventory_item_product ON inventory_item(product_id);
CREATE INDEX idx_inventory_item_warehouse ON inventory_item(warehouse_location_id);
CREATE INDEX idx_inventory_item_product_wh ON inventory_item(product_id, warehouse_location_id);
CREATE INDEX idx_inventory_item_supplier ON inventory_item(supplier_id);
CREATE INDEX idx_inventory_item_low_stock ON inventory_item(is_low_stock);

-- ========================================================================
-- Table: inventory_reservations
-- Entity: InventoryReservation.java
-- ========================================================================
CREATE TABLE inventory_reservations (
                                        id BIGINT NOT NULL AUTO_INCREMENT,
                                        sales_order_id BIGINT NOT NULL,
                                        sales_order_line_id BIGINT NOT NULL,
                                        product_id BIGINT NOT NULL,
                                        warehouse_location_id BIGINT NOT NULL,

    -- Quantity reserved
                                        quantity DECIMAL(19, 4) NOT NULL,

    -- Status
                                        status VARCHAR(30) NOT NULL COMMENT 'ENUM: ReservationStatus (ACTIVE, RELEASED, FULFILLED, CANCELLED)',

    -- Audit fields
                                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                        updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                                        PRIMARY KEY (id),
                                        CONSTRAINT fk_invres_product FOREIGN KEY (product_id)
                                            REFERENCES product(id) ON DELETE RESTRICT,
                                        CONSTRAINT fk_invres_warehouse FOREIGN KEY (warehouse_location_id)
                                            REFERENCES warehouse_location(id) ON DELETE RESTRICT,
                                        CONSTRAINT uk_invres_line_product_wh UNIQUE (sales_order_line_id, product_id, warehouse_location_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stock reservations against sales orders';

CREATE INDEX idx_invres_sales_order ON inventory_reservations(sales_order_id);
CREATE INDEX idx_invres_order_line ON inventory_reservations(sales_order_line_id);
CREATE INDEX idx_invres_product_location ON inventory_reservations(product_id, warehouse_location_id);
CREATE INDEX idx_invres_status ON inventory_reservations(status);

-- ========================================================================
-- END OF V4 MIGRATION
-- ========================================================================