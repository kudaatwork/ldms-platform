-- Fleet Driver Marketplace and Signup Type enhancements.

ALTER TABLE fleet_driver
    ADD COLUMN marketplace_visible BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'When true the driver appears in the freelance marketplace for hiring';

ALTER TABLE fleet_driver_signup_request
    ADD COLUMN signup_type VARCHAR(50) NOT NULL DEFAULT 'COMPANY'
        COMMENT 'COMPANY | FREELANCE';

ALTER TABLE fleet_driver_signup_request
    MODIFY COLUMN company_code VARCHAR(50) NULL;
