-- ========================================================================
-- Flyway Migration V7: Returns and GRV Tables (CORRECTED)
-- ========================================================================
-- Purpose: Purchase returns and goods receipt vouchers
-- Reference: PurchaseReturn.java, GoodsReceivedVoucher.java
-- ========================================================================

-- ========================================================================
-- Table: purchase_return
-- Entity: PurchaseReturn.java
-- ========================================================================
CREATE TABLE purchase_return (
                                 id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identification
                                 return_number VARCHAR(50) NOT NULL UNIQUE,

    -- References
                                 purchase_order_id BIGINT NOT NULL,
                                 warehouse_location_id BIGINT NOT NULL,

    -- User tracking
                                 returned_by_user_id BIGINT NOT NULL,

    -- Reason
                                 reason TEXT,

    -- Audit fields
                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                 entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                 PRIMARY KEY (id),
                                 CONSTRAINT fk_return_purchase_order FOREIGN KEY (purchase_order_id)
                                     REFERENCES purchase_order(id) ON DELETE RESTRICT,
                                 CONSTRAINT fk_return_warehouse FOREIGN KEY (warehouse_location_id)
                                     REFERENCES warehouse_location(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Purchase returns to suppliers';

CREATE UNIQUE INDEX ux_purchase_return_number ON purchase_return(return_number);
CREATE INDEX idx_purchase_return_po ON purchase_return(purchase_order_id);
CREATE INDEX idx_purchase_return_warehouse ON purchase_return(warehouse_location_id);
CREATE INDEX idx_purchase_return_created ON purchase_return(created_at);

-- ========================================================================
-- Table: goods_received_voucher
-- Entity: GoodsReceivedVoucher.java
-- ========================================================================
CREATE TABLE goods_received_voucher (
                                        id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identification
                                        grv_number VARCHAR(50) NOT NULL,

    -- References
                                        purchase_order_id BIGINT NOT NULL,
                                        warehouse_location_id BIGINT NOT NULL,

    -- Receiving details
                                        received_by_user_id BIGINT NOT NULL,
                                        received_date DATETIME(6),

    -- Status
                                        status VARCHAR(30) NOT NULL COMMENT 'ENUM: GrvStatus',

    -- Notes
                                        notes TEXT,

    -- Audit fields
                                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                        updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                        entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                        PRIMARY KEY (id),
                                        CONSTRAINT fk_grv_purchase_order FOREIGN KEY (purchase_order_id)
                                            REFERENCES purchase_order(id) ON DELETE RESTRICT,
                                        CONSTRAINT fk_grv_warehouse FOREIGN KEY (warehouse_location_id)
                                            REFERENCES warehouse_location(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Goods received vouchers (Phase G: Delivery & Receiving)';

CREATE UNIQUE INDEX ux_grv_grv_number ON goods_received_voucher(grv_number);
CREATE INDEX idx_grv_purchase_order ON goods_received_voucher(purchase_order_id);
CREATE INDEX idx_grv_warehouse ON goods_received_voucher(warehouse_location_id);
CREATE INDEX idx_grv_received_date ON goods_received_voucher(received_date);
CREATE INDEX idx_grv_status ON goods_received_voucher(status);

-- ========================================================================
-- END OF V7 MIGRATION
-- ========================================================================