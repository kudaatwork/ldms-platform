-- Canonical LDMS user table is `user` (quoted; not mysql.user). Drops `app_user` as a physical name after cut-over.
-- Run after V2. Idempotent for environments already on `user`.

-- ---------------------------------------------------------------------------
-- 1) Drop foreign keys on application tables that reference app_user
-- ---------------------------------------------------------------------------
SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_account'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_account DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_password'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_password DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_preferences'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_preferences DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cn := (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_security'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'app_user'
    LIMIT 1
);
SET @drop := IF(@cn IS NOT NULL, CONCAT('ALTER TABLE user_security DROP FOREIGN KEY `', @cn, '`'), 'SELECT 1');
PREPARE stmt FROM @drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) Rename app_user -> `user` (resolve empty legacy `user` if needed)
-- ---------------------------------------------------------------------------
SET @app_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user'
);
SET @user_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user'
);

-- app_user only: rename
SET @rename1 := IF(@app_exists > 0 AND @user_exists = 0, 'RENAME TABLE app_user TO `user`', 'SELECT 1');
PREPARE r1 FROM @rename1;
EXECUTE r1;
DEALLOCATE PREPARE r1;

-- both exist: if `user` is empty, replace it; otherwise fail loudly
SET @app_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user'
);
SET @user_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user'
);

SET @user_rows := 0;
SET @cnt_sql := IF(@user_exists > 0, 'SELECT COUNT(*) INTO @user_rows FROM `user`', 'SELECT 0 INTO @user_rows FROM DUAL');
PREPARE c1 FROM @cnt_sql;
EXECUTE c1;
DEALLOCATE PREPARE c1;

SET @drop_empty_user := IF(@app_exists > 0 AND @user_exists > 0 AND IFNULL(@user_rows, 0) = 0, 'DROP TABLE `user`', 'SELECT 1');
PREPARE d1 FROM @drop_empty_user;
EXECUTE d1;
DEALLOCATE PREPARE d1;

SET @app_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user'
);
SET @user_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user'
);
SET @rename2 := IF(@app_exists > 0 AND @user_exists = 0, 'RENAME TABLE app_user TO `user`', 'SELECT 1');
PREPARE r2 FROM @rename2;
EXECUTE r2;
DEALLOCATE PREPARE r2;

SET @app_still := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user'
);
SET @user_final := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user'
);
SET @user_rows2 := 0;
SET @cnt2 := IF(@user_final > 0, 'SELECT COUNT(*) INTO @user_rows2 FROM `user`', 'SELECT 0 INTO @user_rows2 FROM DUAL');
PREPARE c2 FROM @cnt2;
EXECUTE c2;
DEALLOCATE PREPARE c2;

-- If app_user still exists alongside non-empty `user`, fail migration (unknown table = explicit stop)
SET @fail_probe := IF(@app_still > 0 AND IFNULL(@user_rows2, 0) > 0,
    'SELECT 1 FROM ldms_flyway_v3_app_user_and_user_both_populated_resolve_manually',
    'SELECT 1');
PREPARE f1 FROM @fail_probe;
EXECUTE f1;
DEALLOCATE PREPARE f1;

-- ---------------------------------------------------------------------------
-- 3) Ensure child tables reference `user` (id)
-- ---------------------------------------------------------------------------
SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_account');
SET @has_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_account'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
);
SET @add_ua := IF(@t_exists > 0 AND @has_user_fk = 0,
    'ALTER TABLE user_account ADD CONSTRAINT fk_um_user_account_user FOREIGN KEY (user_id) REFERENCES `user` (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_ua;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_password');
SET @has_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_password'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
);
SET @add_up := IF(@t_exists > 0 AND @has_user_fk = 0,
    'ALTER TABLE user_password ADD CONSTRAINT fk_um_user_password_user FOREIGN KEY (user_id) REFERENCES `user` (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_up;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_preferences');
SET @has_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_preferences'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
);
SET @add_prefs := IF(@t_exists > 0 AND @has_user_fk = 0,
    'ALTER TABLE user_preferences ADD CONSTRAINT fk_um_user_preferences_user FOREIGN KEY (user_id) REFERENCES `user` (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_prefs;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @t_exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_security');
SET @has_user_fk := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'user_security'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'user'
);
SET @add_sec := IF(@t_exists > 0 AND @has_user_fk = 0,
    'ALTER TABLE user_security ADD CONSTRAINT fk_um_user_security_user FOREIGN KEY (user_id) REFERENCES `user` (id)',
    'SELECT 1');
PREPARE stmt2 FROM @add_sec;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
