-- Support ticket conversation thread and lifecycle timestamps.

ALTER TABLE support_ticket
    ADD COLUMN resolved_at DATETIME(6) NULL AFTER assigned_handler_username,
    ADD COLUMN closed_at DATETIME(6) NULL AFTER resolved_at;

CREATE TABLE support_ticket_message (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    support_ticket_id   BIGINT          NOT NULL,
    author_username     VARCHAR(150)    NOT NULL,
    author_role         VARCHAR(50)     NOT NULL,
    visibility          VARCHAR(50)     NOT NULL DEFAULT 'PUBLIC',
    body                TEXT            NOT NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    KEY idx_support_ticket_message_ticket (support_ticket_id),
    KEY idx_support_ticket_message_created (created_at),
    CONSTRAINT fk_support_ticket_message_ticket
        FOREIGN KEY (support_ticket_id) REFERENCES support_ticket (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
