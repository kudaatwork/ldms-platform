ALTER TABLE ldms_audit_trail_dev_db.audit_log_churn_history
    ADD COLUMN churn_status VARCHAR(50) NULL AFTER newest_request_timestamp,
    ADD COLUMN job_execution_id BIGINT NULL AFTER churn_status,
    ADD COLUMN failure_reason VARCHAR(2500) NULL AFTER job_execution_id,
    ADD COLUMN completed_at DATETIME(6) NULL AFTER failure_reason;

UPDATE ldms_audit_trail_dev_db.audit_log_churn_history
SET churn_status = 'SUCCESS',
    completed_at = COALESCE(completed_at, triggered_at)
WHERE churn_status IS NULL;

ALTER TABLE ldms_audit_trail_dev_db.audit_log_churn_history
    MODIFY churn_status VARCHAR(50) NOT NULL;

ALTER TABLE ldms_audit_trail_dev_db.audit_log_churn_history
    ADD CONSTRAINT chk_audit_log_churn_status CHECK (
        churn_status IN ('RUNNING', 'SUCCESS', 'FAILED')
        );

CREATE INDEX idx_audit_log_churn_job_exec
    ON ldms_audit_trail_dev_db.audit_log_churn_history (job_execution_id);
