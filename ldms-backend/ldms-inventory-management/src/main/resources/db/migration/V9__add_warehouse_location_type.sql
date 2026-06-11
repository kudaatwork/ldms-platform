-- ========================================================================
-- Flyway Migration V9: Add warehouse type to warehouse_location
-- ========================================================================
-- Purpose: Distinguish supplier vs customer warehouse locations
-- Reference: WarehouseLocation.java
-- ========================================================================

ALTER TABLE warehouse_location
    ADD COLUMN warehouse_type VARCHAR(20) NULL COMMENT 'ENUM: SUPPLIER, CUSTOMER';

CREATE INDEX idx_warehouse_location_type ON warehouse_location(warehouse_type);

-- ========================================================================
-- END OF V9 MIGRATION
-- ========================================================================
