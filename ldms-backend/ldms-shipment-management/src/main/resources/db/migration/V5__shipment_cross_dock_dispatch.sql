-- V5: Add cross_dock_dispatch_id to shipment table for cross-dock dispatch source tracking

ALTER TABLE shipment
    ADD COLUMN cross_dock_dispatch_id BIGINT NULL AFTER sales_order_id;

CREATE INDEX idx_shipment_cross_dock_id ON shipment (cross_dock_dispatch_id);
