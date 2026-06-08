-- Phone verification fields on user table + OTP challenge table.

-- 1. Phone verification columns on the user table
ALTER TABLE `user`
    ADD COLUMN `phone_verified`          TINYINT(1) NOT NULL DEFAULT 0     AFTER `email_verified`,
    ADD COLUMN `last_phone_verified_at`  DATETIME(6)                       AFTER `phone_verified`;

-- 2. OTP challenge table (phone verification, login 2FA, step-up auth)
CREATE TABLE `user_otp_challenge` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT          NOT NULL,
    `otp_type`      VARCHAR(50)     NOT NULL,
    `otp_hash`      VARCHAR(255)    NOT NULL,
    `expires_at`    DATETIME(6)     NOT NULL,
    `used`          TINYINT(1)      NOT NULL DEFAULT 0,
    `entity_status` VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    `created_at`    DATETIME(6)     NOT NULL,
    `created_by`    VARCHAR(100)    NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_otp_user_id`         (`user_id`),
    INDEX `idx_otp_user_type_used`  (`user_id`, `otp_type`, `used`),
    CONSTRAINT `fk_otp_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
