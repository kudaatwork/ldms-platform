CREATE TABLE platform_user_notification
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    organization_id  BIGINT,
    event_key        VARCHAR(100)   NOT NULL,
    title            VARCHAR(150)   NOT NULL,
    body             LONGTEXT       NOT NULL,
    action_route     VARCHAR(255),
    entity_type      VARCHAR(50),
    entity_id        BIGINT,
    source_event_id  VARCHAR(100)   NOT NULL,
    read_at          DATETIME(6),
    dismissed_at     DATETIME(6),
    created_at       DATETIME(6),
    created_by       VARCHAR(255),
    modified_at      DATETIME(6),
    modified_by      VARCHAR(255),
    entity_status    VARCHAR(50),
    CONSTRAINT uk_platform_user_notification_user_event UNIQUE (user_id, source_event_id)
);

CREATE INDEX idx_platform_user_notification_inbox
    ON platform_user_notification (user_id, dismissed_at, created_at DESC);
