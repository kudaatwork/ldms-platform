-- Client application that originated the request (admin portal, platform portal, mobile, etc.)
ALTER TABLE ldms_audit_trail_dev_db.audit_log
    ADD COLUMN client_platform VARCHAR(50) NULL AFTER username;

CREATE INDEX idx_audit_client_platform ON ldms_audit_trail_dev_db.audit_log (client_platform);
CREATE INDEX idx_audit_username_time ON ldms_audit_trail_dev_db.audit_log (username, request_timestamp);
