-- ================================================================
-- Service: ldms-fleet-management
-- Version: V5
-- Purpose: Driver personal details — identity documents, driver's
--          licence upload, and residential address fields.
--          Also makes user_id nullable so a driver record can exist
--          before a platform user account is linked.
-- ================================================================

-- 1. Drop the existing unique key that enforces (organization_id, user_id)
--    — must be removed before making user_id nullable, otherwise NULL
--    values would violate the constraint on some MySQL configurations.
ALTER TABLE fleet_driver
    DROP INDEX ux_fleet_driver_org_user;

-- 2. Make user_id nullable (was NOT NULL in V1).
ALTER TABLE fleet_driver
    MODIFY COLUMN user_id BIGINT NULL COMMENT 'Logical reference to ldms_user_management.user; NULL until platform account is linked';

-- 3. Identity document columns
ALTER TABLE fleet_driver
    ADD COLUMN national_id_number      VARCHAR(100)    NULL                                    COMMENT 'National ID / NRC number'           AFTER user_id,
    ADD COLUMN national_id_expiry_date DATE            NULL                                    COMMENT 'National ID expiry date'            AFTER national_id_number,
    ADD COLUMN national_id_upload_id   BIGINT          NULL                                    COMMENT 'file-upload-service upload id'      AFTER national_id_expiry_date,
    ADD COLUMN passport_number         VARCHAR(100)    NULL                                    COMMENT 'Passport number'                    AFTER national_id_upload_id,
    ADD COLUMN passport_expiry_date    DATE            NULL                                    COMMENT 'Passport expiry date'               AFTER passport_number,
    ADD COLUMN passport_upload_id      BIGINT          NULL                                    COMMENT 'file-upload-service upload id'      AFTER passport_expiry_date,
    ADD COLUMN license_upload_id       BIGINT          NULL                                    COMMENT 'Driver licence upload id'           AFTER passport_upload_id;

-- 4. Address columns
ALTER TABLE fleet_driver
    ADD COLUMN address_line1        VARCHAR(200)    NULL    COMMENT 'Street address line 1'   AFTER license_upload_id,
    ADD COLUMN address_line2        VARCHAR(200)    NULL    COMMENT 'Street address line 2'   AFTER address_line1,
    ADD COLUMN address_city         VARCHAR(100)    NULL    COMMENT 'City / town'             AFTER address_line2,
    ADD COLUMN address_province     VARCHAR(100)    NULL    COMMENT 'Province / state'        AFTER address_city,
    ADD COLUMN address_postal_code  VARCHAR(30)     NULL    COMMENT 'Postal / ZIP code'       AFTER address_province,
    ADD COLUMN address_country      VARCHAR(100)    NULL    COMMENT 'Country'                 AFTER address_postal_code;

-- 5. Re-add a non-unique index on user_id to keep lookup performance.
ALTER TABLE fleet_driver
    ADD INDEX idx_fleet_driver_user_id (user_id);
