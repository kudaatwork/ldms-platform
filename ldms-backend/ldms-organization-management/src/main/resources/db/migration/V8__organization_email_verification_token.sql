-- Organisation-level email verification (supplier-registered customers/transporters).

ALTER TABLE organization
    ADD COLUMN email_verification_token VARCHAR(255) NULL AFTER is_verified;
