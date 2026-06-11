-- Phase H: Transfer-backed GRV and shipment linkage
-- 1. Allow GRVs to be created from an inventory transfer (no PO required)
-- 2. Allow an inventory transfer to be linked to a shipment/trip

-- Make purchase_order_id nullable so GRVs can exist without a PO
ALTER TABLE goods_received_voucher
    MODIFY COLUMN purchase_order_id BIGINT NULL;

-- Add FK to inventory_transfer so a GRV can be created as part of a transfer completion
ALTER TABLE goods_received_voucher
    ADD COLUMN inventory_transfer_id BIGINT NULL AFTER purchase_order_id,
    ADD CONSTRAINT fk_grv_inventory_transfer
        FOREIGN KEY (inventory_transfer_id) REFERENCES inventory_transfer (id);

CREATE INDEX idx_grv_inventory_transfer_id ON goods_received_voucher (inventory_transfer_id);

-- Add shipment_id to inventory_transfer so a transfer can be linked to a shipment/trip
ALTER TABLE inventory_transfer
    ADD COLUMN shipment_id BIGINT NULL AFTER reference;
