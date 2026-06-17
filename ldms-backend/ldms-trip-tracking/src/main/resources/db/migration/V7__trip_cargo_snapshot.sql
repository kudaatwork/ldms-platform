-- Denormalise shipment cargo on trip for live tracking and fleet visibility.
ALTER TABLE trip
    ADD COLUMN shipment_number VARCHAR(50) NULL COMMENT 'Denorm from shipment' AFTER shipment_id,
    ADD COLUMN product_code VARCHAR(60) NULL COMMENT 'Denorm from shipment' AFTER product_name,
    ADD COLUMN quantity DECIMAL(19, 4) NULL COMMENT 'Denorm from shipment' AFTER product_code;
