-- API credentials for third-party systems integrating with inventory via api_key auth.
-- api_key is a 64-character UUID-derived token (unique per credential).
-- status: ACTIVE | SUSPENDED.

CREATE TABLE inventory_integration_credential (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id   BIGINT         NOT NULL,
    credential_label  VARCHAR(200)   NOT NULL,
    api_key           VARCHAR(64)    NOT NULL UNIQUE COMMENT '64-char UUID-based token',
    webhook_url       VARCHAR(500)   NULL,
    callback_grv_url  VARCHAR(500)   NULL,
    status            VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | SUSPENDED',
    last_used_at      DATETIME(6)    NULL,
    entity_status     VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME(6)    NULL,
    created_by        VARCHAR(100)   NULL,
    modified_at       DATETIME(6)    NULL,
    modified_by       VARCHAR(100)   NULL,

    INDEX idx_integration_cred_org    (organization_id),
    INDEX idx_integration_cred_status (status, entity_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
