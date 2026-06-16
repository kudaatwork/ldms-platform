-- Cross-border flag for international transfers requiring border clearance.
ALTER TABLE inventory_transfer
    ADD COLUMN cross_border TINYINT(1) NOT NULL DEFAULT 0 AFTER reference;
