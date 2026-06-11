-- Extend low_stock_alert for batch alert metadata mapped on LowStockAlert entity.

ALTER TABLE low_stock_alert
    ADD COLUMN product_id BIGINT NULL AFTER inventory_item_id,
    ADD COLUMN product_name VARCHAR(255) NULL AFTER product_id,
    ADD COLUMN warehouse_location_id BIGINT NULL AFTER product_name,
    ADD COLUMN reorder_quantity DECIMAL(19, 4) NULL AFTER alert_threshold,
    ADD COLUMN severity VARCHAR(30) NULL AFTER alert_sent_at;
