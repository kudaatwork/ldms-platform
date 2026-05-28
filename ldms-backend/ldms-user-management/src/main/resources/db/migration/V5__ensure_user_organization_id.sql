-- Link portal users to organisations (contact person, org staff). Idempotent for legacy schemas.
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'organization_id'
);
SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN organization_id BIGINT NULL AFTER id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND INDEX_NAME = 'idx_user_organization_id'
);
SET @idx := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_user_organization_id ON `user` (organization_id, entity_status)',
    'SELECT 1'
);
PREPARE stmt2 FROM @idx;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
