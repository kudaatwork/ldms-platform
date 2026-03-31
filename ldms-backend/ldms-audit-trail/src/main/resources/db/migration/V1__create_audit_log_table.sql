-- ldms-audit-trail: dedicated schema (not shared with other services).
-- Retention: keep raw audit_log rows for 90 days, then archive or purge (operational job; not enforced here).
-- This table is INSERT-only: no UPDATE or DELETE from application code; no entity_status / audit columns.

CREATE SCHEMA IF NOT EXISTS ldms_audit_trail_dev_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE TABLE ldms_audit_trail_dev_db.audit_log
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    action             VARCHAR(255)   NULL,
    client_ip_address  VARCHAR(255)   NULL,
    curl_command       LONGTEXT       NULL,
    event_type         VARCHAR(50)    NULL,
    exception_message  LONGTEXT       NULL,
    http_method        VARCHAR(255)   NULL,
    http_status_code   INT            NULL,
    request_headers    TEXT           NULL,
    request_payload    LONGTEXT       NULL,
    request_url        VARCHAR(255)   NULL,
    response_payload   LONGTEXT       NULL,
    response_time_ms   BIGINT         NULL,
    service_name       VARCHAR(255)   NULL,
    request_timestamp  DATETIME(6)    NULL,
    response_timestamp DATETIME(6)    NULL,
    trace_id           VARCHAR(255)   NULL,
    username           VARCHAR(255)   NULL,
    CONSTRAINT chk_audit_log_event_type CHECK (
        event_type IS NULL
        OR event_type IN ('EXCEPTION', 'FEIGN_CALL', 'SERVICE_METHOD', 'WEB_REQUEST')
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Performance indexes for query API filter combinations
CREATE INDEX idx_audit_service_name ON ldms_audit_trail_dev_db.audit_log (service_name);
CREATE INDEX idx_audit_event_type ON ldms_audit_trail_dev_db.audit_log (event_type);
CREATE INDEX idx_audit_username ON ldms_audit_trail_dev_db.audit_log (username);
CREATE INDEX idx_audit_trace_id ON ldms_audit_trail_dev_db.audit_log (trace_id);
CREATE INDEX idx_audit_req_timestamp ON ldms_audit_trail_dev_db.audit_log (request_timestamp);
CREATE INDEX idx_audit_http_status ON ldms_audit_trail_dev_db.audit_log (http_status_code);
CREATE INDEX idx_audit_request_url ON ldms_audit_trail_dev_db.audit_log (request_url(200));
CREATE INDEX idx_audit_service_time ON ldms_audit_trail_dev_db.audit_log (service_name, request_timestamp);
