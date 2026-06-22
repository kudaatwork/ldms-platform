-- Multi-stage payment verification policy (1–3 billing approvers) and review audit trail

ALTER TABLE organization_billing_setting
    ADD COLUMN required_payment_verification_stages INT NOT NULL DEFAULT 1
        COMMENT 'Sequential billing approvers required before payment is verified (1-3)'
        AFTER low_balance_threshold_cents;

ALTER TABLE payment
    ADD COLUMN current_verification_stage INT NOT NULL DEFAULT 0 AFTER verified_by,
    ADD COLUMN required_verification_stages INT NULL AFTER current_verification_stage;

CREATE TABLE payment_verification_review (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    payment_id                  BIGINT          NOT NULL,
    stage_number                INT             NOT NULL,
    reviewed_by_user_id         BIGINT          NULL,
    reviewed_by_username        VARCHAR(100)    NOT NULL,
    reviewed_at                 DATETIME(6)     NOT NULL,
    notes                       VARCHAR(500)    NULL,
    entity_status               VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at                  DATETIME(6)     NOT NULL,
    created_by                  VARCHAR(100)    NOT NULL,
    modified_at                 DATETIME(6)     NULL,
    modified_by                 VARCHAR(100)    NULL,
    PRIMARY KEY (id),
    INDEX idx_payment_verification_review_payment (payment_id, entity_status)
);
