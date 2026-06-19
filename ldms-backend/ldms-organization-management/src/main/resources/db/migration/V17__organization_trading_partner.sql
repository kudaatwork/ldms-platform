-- V17: Trading partner directory.
--
-- Allows an organisation to maintain a CRM-style directory of customers and suppliers.
-- A partner may be a bare record (record_only = true, linked_organization_id null) or a link
-- to another LDMS platform organisation (linked_organization_id set, record_only false).

CREATE TABLE organization_trading_partner (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id         BIGINT          NOT NULL COMMENT 'Owning organisation',
    partner_role            VARCHAR(50)     NOT NULL COMMENT 'CUSTOMER | SUPPLIER',
    name                    VARCHAR(300)    NOT NULL,
    email                   VARCHAR(255)    NULL,
    phone                   VARCHAR(50)     NULL,
    location_id             BIGINT          NULL     COMMENT 'ldms-locations address id',
    notes                   TEXT            NULL,
    linked_organization_id  BIGINT          NULL     COMMENT 'Platform org id when partner is on LDMS',
    record_only             BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'True = CRM entry only; false = linked platform org',
    entity_status           VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME(6)     NOT NULL,
    created_by              VARCHAR(150)    NOT NULL,
    modified_at             DATETIME(6)     NULL,
    modified_by             VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    INDEX idx_otp_organization   (organization_id),
    INDEX idx_otp_partner_role   (partner_role),
    INDEX idx_otp_linked_org     (linked_organization_id),
    INDEX idx_otp_entity_status  (entity_status),
    CONSTRAINT fk_otp_organization FOREIGN KEY (organization_id) REFERENCES organization (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
