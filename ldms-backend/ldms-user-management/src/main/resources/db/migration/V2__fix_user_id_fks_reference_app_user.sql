-- JPA maps User to `app_user`. Legacy DBs may have `user_id` FKs referencing table `user`, which breaks
-- inserts: new users land in `app_user` while `user_account` etc. still enforce FK against `user`.

-- user_account.user_id
SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_account'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_account DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account');
SET @has_app_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_account'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
);
SET @add_ua := IF(@t_exists > 0 AND @has_app_user_fk = 0,
    'ALTER TABLE user_account ADD CONSTRAINT fk_um_user_account_user FOREIGN KEY (user_id) REFERENCES app_user (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_ua;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- user_password.user_id
SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_password'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_password DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_password');
SET @has_app_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_password'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
);
SET @add_up := IF(@t_exists > 0 AND @has_app_user_fk = 0,
    'ALTER TABLE user_password ADD CONSTRAINT fk_um_user_password_user FOREIGN KEY (user_id) REFERENCES app_user (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_up;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- user_preferences.user_id
SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_preferences'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_preferences DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_preferences');
SET @has_app_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_preferences'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
);
SET @add_prefs := IF(@t_exists > 0 AND @has_app_user_fk = 0,
    'ALTER TABLE user_preferences ADD CONSTRAINT fk_um_user_preferences_user FOREIGN KEY (user_id) REFERENCES app_user (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_prefs;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- user_security.user_id
SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_security'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_security DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_security');
SET @has_app_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_security'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
);
SET @add_sec := IF(@t_exists > 0 AND @has_app_user_fk = 0,
    'ALTER TABLE user_security ADD CONSTRAINT fk_um_user_security_user FOREIGN KEY (user_id) REFERENCES app_user (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_sec;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
