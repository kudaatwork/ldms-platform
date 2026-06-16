-- Support bought-goods (sales order) shipments alongside inventory transfers.

ALTER TABLE shipment
    MODIFY COLUMN inventory_transfer_id BIGINT NULL,
    ADD COLUMN sales_order_id BIGINT NULL AFTER inventory_transfer_id,
    ADD COLUMN purchase_order_id BIGINT NULL AFTER sales_order_id,
    ADD COLUMN customer_organization_id BIGINT NULL AFTER purchase_order_id;

CREATE INDEX idx_shipment_sales_order_id ON shipment (sales_order_id);

ALTER TABLE border_clearance_case
    MODIFY COLUMN inventory_transfer_id BIGINT NULL,
    ADD COLUMN sales_order_id BIGINT NULL AFTER inventory_transfer_id;

CREATE INDEX idx_bcc_sales_order_id ON border_clearance_case (sales_order_id);
