-- Align with SalesReservation entity (@Table name = sales_reservations).
-- V8 created legacy sales_reservation (singular) with a different shape; keep it for now.

CREATE TABLE sales_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reservation_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_location_id BIGINT NOT NULL,
    quantity_reserved DECIMAL(19, 4) NOT NULL,
    reserved_until DATETIME(6) NULL,
    reservation_status VARCHAR(50) NULL COMMENT 'ENUM: ReservationStatus',
    created_by_user_id BIGINT NOT NULL,
    updated_by_user_id BIGINT NULL,
    notes TEXT NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_sales_reservations_reservation_number UNIQUE (reservation_number),
    CONSTRAINT fk_sales_reservations_product FOREIGN KEY (product_id)
        REFERENCES product(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_reservations_warehouse FOREIGN KEY (warehouse_location_id)
        REFERENCES warehouse_location(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Customer sales reservations against warehouse stock';

CREATE INDEX idx_sales_reservation_status_until
    ON sales_reservations(reservation_status, reserved_until);

CREATE INDEX idx_sales_reservation_product_wh
    ON sales_reservations(product_id, warehouse_location_id);

CREATE INDEX idx_sales_reservations_customer
    ON sales_reservations(customer_id);
