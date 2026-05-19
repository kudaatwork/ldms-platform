ALTER TABLE notification_log
    ADD COLUMN event_id VARCHAR(64) NULL AFTER id,
    ADD COLUMN recipient_email VARCHAR(255) NULL AFTER recipient_id,
    ADD COLUMN recipient_phone VARCHAR(50) NULL AFTER recipient_email;

CREATE INDEX idx_notification_log_event_id ON notification_log (event_id);
CREATE INDEX idx_notification_log_status ON notification_log (status);
CREATE INDEX idx_notification_log_created_at ON notification_log (created_at);
