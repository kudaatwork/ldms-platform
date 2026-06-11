-- Align inventory_discrepancy with InventoryDiscrepancy entity fields used by batch reconciliation.

ALTER TABLE inventory_discrepancy
    ADD COLUMN product_id BIGINT NULL AFTER inventory_item_id,
    ADD COLUMN warehouse_location_id BIGINT NULL AFTER product_id;

CREATE INDEX idx_discrepancy_product ON inventory_discrepancy(product_id);
CREATE INDEX idx_discrepancy_warehouse ON inventory_discrepancy(warehouse_location_id);
