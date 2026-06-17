-- Public demo booking requests from the platform portal landing / contact page.

CREATE TABLE demo_requisition (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    requisition_number      VARCHAR(32)     NOT NULL,
    full_name               VARCHAR(200)    NOT NULL,
    email                   VARCHAR(254)    NOT NULL,
    phone                   VARCHAR(50)     NOT NULL,
    address                 VARCHAR(500)    NOT NULL,
    demo_request            TEXT            NOT NULL,
    status                  VARCHAR(50)     NOT NULL DEFAULT 'NEW',
    assigned_handler_username VARCHAR(150)  NULL,
    admin_notes             TEXT            NULL,
    contacted_at            DATETIME(6)     NULL,
    scheduled_at            DATETIME(6)     NULL,
    completed_at            DATETIME(6)     NULL,
    entity_status           VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME(6)     NOT NULL,
    created_by              VARCHAR(150)    NOT NULL,
    modified_at             DATETIME(6)     NULL,
    modified_by             VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_demo_requisition_number (requisition_number),
    KEY idx_demo_requisition_status (status),
    KEY idx_demo_requisition_email (email),
    KEY idx_demo_requisition_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
