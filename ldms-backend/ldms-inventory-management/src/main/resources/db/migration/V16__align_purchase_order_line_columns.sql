-- Align purchase_order_line with PurchaseOrderLine entity (JPA schema validation).

ALTER TABLE purchase_order_line
    ADD COLUMN supplier_product_code VARCHAR(100) NULL AFTER product_id,
    ADD COLUMN unit_of_measure VARCHAR(50) NOT NULL DEFAULT 'EACH' COMMENT 'ENUM: UnitOfMeasure' AFTER supplier_product_code;
