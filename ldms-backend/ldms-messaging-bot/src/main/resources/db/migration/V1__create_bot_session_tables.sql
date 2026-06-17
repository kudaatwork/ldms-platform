CREATE TABLE bot_session (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    session_id          VARCHAR(32)     NOT NULL,
    requester_username  VARCHAR(150)    NOT NULL,
    user_display_name   VARCHAR(200)    NOT NULL,
    user_phone          VARCHAR(40)     NULL,
    organization_id     BIGINT          NULL,
    organization_name   VARCHAR(200)    NULL,
    channel             VARCHAR(50)     NOT NULL DEFAULT 'WEB',
    status              VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    topic               VARCHAR(200)    NULL,
    satisfaction_score  INT             NULL,
    escalated_ticket_id BIGINT          NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bot_session_session_id (session_id),
    KEY idx_bot_session_requester (requester_username),
    KEY idx_bot_session_status (status),
    KEY idx_bot_session_modified (modified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bot_message (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    bot_session_id      BIGINT          NOT NULL,
    role                VARCHAR(50)     NOT NULL,
    body                TEXT            NOT NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    KEY idx_bot_message_session (bot_session_id),
    CONSTRAINT fk_bot_message_session FOREIGN KEY (bot_session_id) REFERENCES bot_session (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
