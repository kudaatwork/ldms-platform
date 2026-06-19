-- Driver self-service signup request.
-- Drivers submit a request to join a transporter organisation by company code.
-- Platform operators review and approve/reject these requests.

CREATE TABLE fleet_driver_signup_request (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT          NULL,
    company_code    VARCHAR(50)     NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(200)    NOT NULL,
    phone_number    VARCHAR(50)     NOT NULL,
    license_number  VARCHAR(100)    NOT NULL,
    license_class   VARCHAR(50)     NOT NULL,
    status          VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    entity_status   VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)    NOT NULL,
    modified_at     DATETIME(6)     NULL,
    modified_by     VARCHAR(150)    NULL,
    UNIQUE KEY uk_signup_email (email),
    INDEX idx_signup_status (status, entity_status)
);
