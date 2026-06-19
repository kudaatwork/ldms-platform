-- Admin-managed FAQ entries for bot RAG context injection.

CREATE TABLE bot_faq (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    question        VARCHAR(500)    NOT NULL,
    answer          TEXT            NOT NULL,
    category        VARCHAR(50)     NOT NULL DEFAULT 'GENERAL',
    keywords        VARCHAR(500)    NULL COMMENT 'Comma-separated search hints for retrieval',
    published       TINYINT(1)      NOT NULL DEFAULT 1,
    use_count       BIGINT          NOT NULL DEFAULT 0,
    entity_status   VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)    NOT NULL,
    modified_at     DATETIME(6)     NULL,
    modified_by     VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    INDEX idx_bot_faq_status_published (entity_status, published),
    INDEX idx_bot_faq_category (category)
);

INSERT INTO bot_faq (question, answer, category, keywords, published, use_count, entity_status, created_at, created_by)
VALUES
(
    'How do I track a shipment?',
    'Open Track shipments in the platform portal to see allocation, trip status, and live map when GPS is active. Drivers use the driver workspace; operators use the trip tracking workspace.',
    'OPERATIONS',
    'shipment,track,live map,trip',
    1,
    0,
    'ACTIVE',
    NOW(6),
    'system'
),
(
    'How does driver onboarding work?',
    'Drivers register via Join as driver, upload ID and licence documents, and wait for transporter approval. Approved drivers receive temporary credentials by email and must set a permanent password on first sign-in.',
    'ONBOARDING',
    'driver,signup,onboarding,credentials',
    1,
    0,
    'ACTIVE',
    NOW(6),
    'system'
);
