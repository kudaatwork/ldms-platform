ALTER TABLE user
    ADD COLUMN must_change_credentials TINYINT(1) NOT NULL DEFAULT 0 AFTER email_verified;
