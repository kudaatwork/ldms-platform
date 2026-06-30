-- ========================================================================
-- Flyway Migration V30: Organisation scope for product categories
-- ========================================================================
-- Owning organisation is stored in supplier_id (same convention as product).
-- ========================================================================

ALTER TABLE product_category
    ADD COLUMN supplier_id BIGINT NULL COMMENT 'Owning organisation (FK to Org Service)' AFTER description;

CREATE INDEX idx_product_category_supplier ON product_category (supplier_id);

-- Backfill from products that reference each category.
UPDATE product_category pc
    INNER JOIN (
        SELECT category_id, MIN(supplier_id) AS supplier_id
        FROM product
        WHERE entity_status <> 'DELETED'
          AND category_id IS NOT NULL
          AND supplier_id IS NOT NULL
        GROUP BY category_id
    ) owned ON owned.category_id = pc.id
SET pc.supplier_id = owned.supplier_id
WHERE pc.supplier_id IS NULL;

-- ========================================================================
-- END OF V30 MIGRATION
-- ========================================================================
