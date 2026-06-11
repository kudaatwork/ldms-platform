-- ========================================================================
-- Flyway Migration V10: Backfill warehouse_location.warehouse_type
-- ========================================================================
-- Purpose: Tag existing warehouse records for supplier/customer usage
-- Rule:
--   - SUPPLIER when supplier_id is present
--   - CUSTOMER when supplier_id is NULL
-- ========================================================================

UPDATE warehouse_location
SET warehouse_type = CASE
    WHEN supplier_id IS NULL THEN 'CUSTOMER'
    ELSE 'SUPPLIER'
END
WHERE warehouse_type IS NULL;

-- ========================================================================
-- END OF V10 MIGRATION
-- ========================================================================
