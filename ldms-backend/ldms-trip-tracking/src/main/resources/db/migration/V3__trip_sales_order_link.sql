ALTER TABLE trip
    ADD COLUMN sales_order_id BIGINT NULL AFTER inventory_transfer_id;

CREATE INDEX idx_trip_sales_order_id ON trip (sales_order_id);
