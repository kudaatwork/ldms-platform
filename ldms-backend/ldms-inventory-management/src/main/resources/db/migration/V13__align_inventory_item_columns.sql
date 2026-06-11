-- Align inventory_item with InventoryItem entity fields used by the service layer.

ALTER TABLE inventory_item
    ADD COLUMN batch_lot VARCHAR(100) NULL AFTER reorder_quantity,
    ADD COLUMN serial_number VARCHAR(100) NULL AFTER batch_lot,
    ADD COLUMN expires_at DATE NULL AFTER updated_at,
    ADD COLUMN created_by_user_id BIGINT NULL AFTER expires_at,
    ADD COLUMN updated_by_user_id BIGINT NULL AFTER created_by_user_id;

-- Entity maps minStockLevel to reorder_level in code; add alias column if reorder_level exists without min_stock_level.
-- reorder_level already exists from V4 — no rename needed once entity uses @Column(name = "reorder_level").
