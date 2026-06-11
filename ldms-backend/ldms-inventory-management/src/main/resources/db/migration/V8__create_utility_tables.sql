-- ========================================================================
-- Flyway Migration V8: Utility and Supporting Tables (CORRECTED)
-- ========================================================================
-- Purpose: Outbox pattern, idempotency, and utility tables
-- Reference: OutboxEvent.java, IdempotencyKey.java
-- ========================================================================

-- ========================================================================
-- Table: outbox_events
-- Entity: OutboxEvent.java
-- Purpose: Transactional outbox pattern for event publishing
-- ========================================================================
CREATE TABLE outbox_events (
                               id BIGINT NOT NULL AUTO_INCREMENT,

    -- Event identification
                               aggregate_type VARCHAR(100) NOT NULL COMMENT 'e.g., PurchaseOrder, SalesOrder',
                               aggregate_id VARCHAR(100) NOT NULL COMMENT 'ID of the entity that changed',
                               event_type VARCHAR(150) NOT NULL COMMENT 'e.g., po.created, so.confirmed',

    -- Event payload
                               payload LONGTEXT NOT NULL COMMENT 'JSON string with event data',

    -- Status tracking
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'ENUM: OutboxStatus (PENDING, SENT, FAILED)',

    -- Timestamps
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               PRIMARY KEY (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Transactional outbox for reliable event publishing';

CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_created ON outbox_events(created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_event_type ON outbox_events(event_type);

-- ========================================================================
-- Table: idempotency_key
-- Entity: IdempotencyKey.java (extends BaseEntity)
-- Purpose: Prevent duplicate processing of operations
-- ========================================================================
CREATE TABLE idempotency_key (
                                 id BIGINT NOT NULL AUTO_INCREMENT,

    -- Key identification
                                 key_value VARCHAR(200) NOT NULL COMMENT 'Unique idempotency key from client',

    -- Operation details
                                 operation VARCHAR(100) COMMENT 'ENUM: IdempotencyOperation (e.g., RECEIVE_GOODS, COMPLETE_TRANSFER)',
                                 reference_type VARCHAR(100) COMMENT 'e.g., GRV, INVENTORY_TRANSFER',
                                 reference_id BIGINT COMMENT 'ID of created/affected record',

    -- Status
                                 status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'ENUM: IdempotencyStatus',

    -- Cached response for replay
                                 response_status_code INT,
                                 response_body LONGTEXT COMMENT 'JSON serialized response',

    -- Audit fields (from BaseEntity)
                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                 entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                 PRIMARY KEY (id),
                                 CONSTRAINT uk_idempotency_key_value UNIQUE (key_value)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Idempotency keys for duplicate prevention';

CREATE UNIQUE INDEX ux_idempotency_key_value ON idempotency_key(key_value);
CREATE INDEX idx_idempotency_operation ON idempotency_key(operation);
CREATE INDEX idx_idempotency_status ON idempotency_key(status);
CREATE INDEX idx_idempotency_reference ON idempotency_key(reference_type, reference_id);

-- ========================================================================
-- Table: low_stock_alert (Optional - if you're using this entity)
-- ========================================================================
CREATE TABLE low_stock_alert (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 inventory_item_id BIGINT NOT NULL,
                                 alert_threshold DECIMAL(19, 4) NOT NULL,
                                 current_quantity DECIMAL(19, 4) NOT NULL,
                                 alert_sent_at DATETIME(6),

                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                 PRIMARY KEY (id),
                                 CONSTRAINT fk_low_stock_inventory FOREIGN KEY (inventory_item_id)
                                     REFERENCES inventory_item(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Low stock alerts for monitoring';

CREATE INDEX idx_low_stock_inventory ON low_stock_alert(inventory_item_id);
CREATE INDEX idx_low_stock_sent ON low_stock_alert(alert_sent_at);

-- ========================================================================
-- Table: inventory_discrepancy (Optional - if you're using this entity)
-- ========================================================================
CREATE TABLE inventory_discrepancy (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       inventory_item_id BIGINT NOT NULL,
                                       expected_quantity DECIMAL(19, 4) NOT NULL,
                                       actual_quantity DECIMAL(19, 4) NOT NULL,
                                       variance DECIMAL(19, 4) NOT NULL,
                                       reason VARCHAR(500),

                                       discovered_by_user_id BIGINT,
                                       discovered_at DATETIME(6),

                                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                       entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                       PRIMARY KEY (id),
                                       CONSTRAINT fk_discrepancy_inventory FOREIGN KEY (inventory_item_id)
                                           REFERENCES inventory_item(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Inventory discrepancies for cycle counting';

CREATE INDEX idx_discrepancy_inventory ON inventory_discrepancy(inventory_item_id);
CREATE INDEX idx_discrepancy_discovered ON inventory_discrepancy(discovered_at);

-- ========================================================================
-- Table: sales_reservation (Optional - if different from inventory_reservations)
-- Entity: SalesReservation.java
-- ========================================================================
CREATE TABLE sales_reservation (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   sales_order_id BIGINT NOT NULL,
                                   product_id BIGINT NOT NULL,
                                   warehouse_location_id BIGINT NOT NULL,

                                   reserved_quantity DECIMAL(19, 4) NOT NULL,
                                   status VARCHAR(30) NOT NULL COMMENT 'ENUM: ReservationStatus',
                                   reservation_type VARCHAR(30) COMMENT 'ENUM: ReservationType',

                                   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                   entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_sales_res_product FOREIGN KEY (product_id)
                                       REFERENCES product(id) ON DELETE RESTRICT,
                                   CONSTRAINT fk_sales_res_warehouse FOREIGN KEY (warehouse_location_id)
                                       REFERENCES warehouse_location(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Sales-specific reservations (if different from inventory_reservations)';

CREATE INDEX idx_sales_res_so ON sales_reservation(sales_order_id);
CREATE INDEX idx_sales_res_product ON sales_reservation(product_id);
CREATE INDEX idx_sales_res_warehouse ON sales_reservation(warehouse_location_id);
CREATE INDEX idx_sales_res_status ON sales_reservation(status);

-- ========================================================================
-- END OF V8 MIGRATION
-- ========================================================================