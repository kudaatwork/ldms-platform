CREATE TABLE ldms_audit_trail_dev_db.audit_log_churn_history
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_reference          VARCHAR(64)    NOT NULL,
    trigger_type             VARCHAR(50)    NOT NULL,
    triggered_by             VARCHAR(255)   NOT NULL,
    triggered_at             DATETIME(6)    NOT NULL,
    deleted_log_count        BIGINT         NOT NULL,
    oldest_request_timestamp DATETIME(6)    NULL,
    newest_request_timestamp DATETIME(6)    NULL,
    entity_status            VARCHAR(50)    NOT NULL,
    created_at               DATETIME(6)    NOT NULL,
    created_by               VARCHAR(255)   NOT NULL,
    modified_at              DATETIME(6)    NOT NULL,
    modified_by              VARCHAR(255)   NOT NULL,
    CONSTRAINT chk_audit_log_churn_trigger_type CHECK (
        trigger_type IN ('MANUAL', 'SCHEDULED', 'SYSTEM')
    ),
    CONSTRAINT chk_audit_log_churn_entity_status CHECK (
        entity_status IN ('ACTIVE', 'INACTIVE', 'DELETED', 'ARCHIVED')
    ),
    CONSTRAINT uq_audit_log_churn_batch_reference UNIQUE (batch_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_log_churn_triggered_at
    ON ldms_audit_trail_dev_db.audit_log_churn_history (triggered_at);
