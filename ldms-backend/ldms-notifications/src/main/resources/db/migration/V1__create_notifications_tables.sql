CREATE TABLE notification_template
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_key             VARCHAR(100)   NOT NULL,
    description              VARCHAR(255)   NOT NULL,
    channels                 JSON           NOT NULL,
    email_subject            VARCHAR(255),
    email_body_html          LONGTEXT,
    sms_body                 VARCHAR(320),
    in_app_title             VARCHAR(150),
    in_app_body              LONGTEXT,
    whatsapp_template_name   VARCHAR(255),
    is_active                TINYINT(1)     NOT NULL DEFAULT 1,
    created_at               DATETIME(6),
    updated_at               DATETIME(6),
    entity_status            VARCHAR(50),
    CONSTRAINT uk_notification_template_key UNIQUE (template_key)
);

CREATE TABLE notification_log
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id          VARCHAR(255),
    template_key          VARCHAR(255)   NOT NULL,
    channel               VARCHAR(50)    NOT NULL,
    status                VARCHAR(255)   NOT NULL,
    provider              VARCHAR(255),
    provider_message_id   VARCHAR(255),
    payload               JSON,
    rendered_content      LONGTEXT,
    error_message         LONGTEXT,
    created_at            DATETIME(6),
    updated_at            DATETIME(6),
    entity_status         VARCHAR(50)
);

CREATE TABLE user_notification_preference
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        VARCHAR(255)   NOT NULL,
    template_key   VARCHAR(255)   NOT NULL,
    channel        VARCHAR(50)    NOT NULL,
    is_enabled     TINYINT(1)     NOT NULL DEFAULT 1,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    entity_status  VARCHAR(50),
    CONSTRAINT uk_user_notification_preference UNIQUE (user_id, template_key, channel)
);
