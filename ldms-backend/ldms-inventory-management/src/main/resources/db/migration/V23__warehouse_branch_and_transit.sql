-- Branch allocation, virtual transit warehouses, and extended warehouse types

ALTER TABLE warehouse_location
    ADD COLUMN branch_id BIGINT NULL COMMENT 'FK logical ref to organization_branch.id' AFTER supplier_id,
    ADD COLUMN is_virtual BOOLEAN NOT NULL DEFAULT FALSE AFTER warehouse_type;

CREATE INDEX idx_warehouse_location_branch ON warehouse_location (branch_id);
CREATE INDEX idx_warehouse_location_virtual ON warehouse_location (is_virtual);
