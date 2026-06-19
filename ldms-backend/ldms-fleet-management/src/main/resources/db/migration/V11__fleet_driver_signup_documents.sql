-- Driver signup identity documents (national ID + licence, front and back).

ALTER TABLE fleet_driver_signup_request
    ADD COLUMN national_id_number VARCHAR(100) NULL,
    ADD COLUMN staging_session_id BIGINT NULL,
    ADD COLUMN national_id_front_upload_id BIGINT NULL,
    ADD COLUMN national_id_back_upload_id BIGINT NULL,
    ADD COLUMN license_front_upload_id BIGINT NULL,
    ADD COLUMN license_back_upload_id BIGINT NULL;

ALTER TABLE fleet_driver
    ADD COLUMN national_id_back_upload_id BIGINT NULL,
    ADD COLUMN license_back_upload_id BIGINT NULL;
