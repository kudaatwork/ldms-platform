-- ========================================================================
-- Flyway Migration V6: Stock Management Tables (CORRECTED)
-- ========================================================================
-- Purpose: Stock transactions, transfers, and adjustments
-- Reference: StockTransactionHistory.java, InventoryTransfer.java,
--            StockAdjustment.java
-- ========================================================================

-- ========================================================================
-- Table: stock_transaction_history
-- Entity: StockTransactionHistory.java
-- ========================================================================
CREATE TABLE stock_transaction_history (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           inventory_item_id BIGINT NOT NULL,

    -- Transaction details
                                           transaction_type VARCHAR(30) NOT NULL COMMENT 'ENUM: TransactionType',
                                           quantity_change DECIMAL(19, 4) NOT NULL COMMENT 'Positive for IN, Negative for OUT',
                                           unit_cost DECIMAL(19, 4) COMMENT 'Cost per unit at time of transaction',
                                           timestamp DATETIME(6) NOT NULL,

    -- Location
                                           warehouse_location_id BIGINT,

    -- User tracking
                                           performed_by_user_id BIGINT,

    -- Document reference
                                           reference_document_id BIGINT,
                                           reference_document_type VARCHAR(50) COMMENT 'ENUM: ReferenceDocumentType',

    -- Reason
                                           reason VARCHAR(500),

    -- Audit fields
                                           created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                           updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                           entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                           PRIMARY KEY (id),
                                           CONSTRAINT fk_sth_inventory_item FOREIGN KEY (inventory_item_id)
                                               REFERENCES inventory_item(id) ON DELETE RESTRICT,
                                           CONSTRAINT fk_sth_warehouse FOREIGN KEY (warehouse_location_id)
                                               REFERENCES warehouse_location(id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail of all stock movements';

CREATE INDEX idx_sth_inventory_item ON stock_transaction_history(inventory_item_id);
CREATE INDEX idx_sth_warehouse ON stock_transaction_history(warehouse_location_id);
CREATE INDEX idx_sth_transaction_type ON stock_transaction_history(transaction_type);
CREATE INDEX idx_sth_timestamp ON stock_transaction_history(timestamp);
CREATE INDEX idx_sth_reference ON stock_transaction_history(reference_document_type, reference_document_id);

-- ========================================================================
-- Table: inventory_transfer
-- Entity: InventoryTransfer.java
-- ========================================================================
CREATE TABLE inventory_transfer (
                                    id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identification
                                    transfer_number VARCHAR(50) NOT NULL,

    -- Product and locations
                                    product_id BIGINT NOT NULL,
                                    from_location_id BIGINT,
                                    to_location_id BIGINT,

    -- Quantity and cost
                                    quantity DECIMAL(19, 4) NOT NULL,
                                    unit_cost DECIMAL(19, 4) NOT NULL COMMENT 'For WAC calculation',

    -- Status
                                    status VARCHAR(30) NOT NULL COMMENT 'ENUM: TransferStatus',

    -- Reference
                                    reference VARCHAR(200),

    -- Audit fields
                                    created_by_user_id BIGINT,
                                    updated_by_user_id BIGINT,
                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                    PRIMARY KEY (id),
                                    CONSTRAINT fk_transfer_product FOREIGN KEY (product_id)
                                        REFERENCES product(id) ON DELETE RESTRICT,
                                    CONSTRAINT fk_transfer_from_location FOREIGN KEY (from_location_id)
                                        REFERENCES warehouse_location(id) ON DELETE SET NULL,
                                    CONSTRAINT fk_transfer_to_location FOREIGN KEY (to_location_id)
                                        REFERENCES warehouse_location(id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Warehouse-to-warehouse stock transfers';

CREATE UNIQUE INDEX ux_inventory_transfer_transfer_number ON inventory_transfer(transfer_number);
CREATE INDEX idx_transfer_product ON inventory_transfer(product_id);
CREATE INDEX idx_transfer_from_location ON inventory_transfer(from_location_id);
CREATE INDEX idx_transfer_to_location ON inventory_transfer(to_location_id);
CREATE INDEX idx_transfer_status ON inventory_transfer(status);

-- ========================================================================
-- Table: stock_adjustments
-- Entity: StockAdjustment.java
-- ========================================================================
CREATE TABLE stock_adjustments (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   inventory_item_id BIGINT NOT NULL,

    -- Adjustment details
                                   quantity_delta DECIMAL(19, 4) NOT NULL COMMENT 'Can be positive or negative',
                                   unit_cost DECIMAL(19, 4) DEFAULT 0.0000 COMMENT 'Critical for opening stock',
                                   reason VARCHAR(500),

    -- Tracking
                                   adjusted_by_user_id BIGINT NOT NULL,
                                   adjusted_at DATETIME(6) NOT NULL,

    -- Audit fields
                                   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                   entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_adjustment_inventory_item FOREIGN KEY (inventory_item_id)
                                       REFERENCES inventory_item(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Manual stock adjustments and corrections';

CREATE INDEX idx_adjustment_inventory_item ON stock_adjustments(inventory_item_id);
CREATE INDEX idx_adjustment_adjusted_at ON stock_adjustments(adjusted_at);
CREATE INDEX idx_adjustment_user ON stock_adjustments(adjusted_by_user_id);

-- ========================================================================
-- END OF V6 MIGRATION
-- ========================================================================