-- PDF and text knowledge documents uploaded by platform admins for bot RAG context.

CREATE TABLE bot_knowledge_document (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    title               VARCHAR(255)    NOT NULL,
    original_filename   VARCHAR(255)    NOT NULL,
    content_type        VARCHAR(100)    NOT NULL DEFAULT 'application/pdf',
    extracted_text      LONGTEXT        NULL     COMMENT 'Full text extracted from uploaded file',
    file_size_bytes     BIGINT          NOT NULL DEFAULT 0,
    published           TINYINT(1)      NOT NULL DEFAULT 1,
    use_count           BIGINT          NOT NULL DEFAULT 0,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    INDEX idx_bot_knowledge_doc_status_published (entity_status, published),
    INDEX idx_bot_knowledge_doc_title (title)
);
