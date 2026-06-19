-- ============================================================
-- V8: Trip Delivery Workflow
--
-- Adds:
--   trip_delivery_workflow   - per-trip delivery workflow state machine
--   trip_delivery_return_line - individual return line items
--
-- Alters:
--   delivery_otp             - adds otp_channel, recipient_contact;
--                              makes sent_to_user_id nullable
-- ============================================================

-- --------------------------------------------------------
-- 1. delivery_otp: add channel + recipient columns and
--    relax the NOT NULL constraint on sent_to_user_id
-- --------------------------------------------------------
ALTER TABLE delivery_otp
    MODIFY COLUMN sent_to_user_id BIGINT NULL,
    ADD COLUMN otp_channel       VARCHAR(50)  NULL COMMENT 'SMS | WHATSAPP | EMAIL' AFTER sent_to_user_id,
    ADD COLUMN recipient_contact VARCHAR(320) NULL COMMENT 'Phone number or email address' AFTER otp_channel;

-- --------------------------------------------------------
-- 2. trip_delivery_workflow
--    One row per trip; created when the driver triggers arrival.
-- --------------------------------------------------------
CREATE TABLE trip_delivery_workflow (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    trip_id                     BIGINT          NOT NULL,

    -- Counting timestamps — populated as each actor starts/finishes
    driver_counting_started_at  DATETIME(6)     NULL,
    driver_counting_finished_at DATETIME(6)     NULL,
    customer_counting_started_at DATETIME(6)    NULL,
    customer_counting_finished_at DATETIME(6)   NULL,

    -- Counted quantity captured at finish-counting stage
    expected_quantity           DECIMAL(19, 2)  NULL,
    counted_quantity            DECIMAL(19, 2)  NULL,

    -- OTP channel info stored here for delivery-otp linkage awareness
    otp_channel                 VARCHAR(50)     NULL COMMENT 'SMS | WHATSAPP | EMAIL',
    otp_recipient               VARCHAR(320)    NULL,

    -- Delivery confirmation
    delivery_notes              VARCHAR(2000)   NULL,

    -- Return journey
    return_initiated_at         DATETIME(6)     NULL,
    return_completed_at         DATETIME(6)     NULL,

    -- Audit
    entity_status               VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at                  DATETIME(6)     NOT NULL,
    created_by                  VARCHAR(150)    NOT NULL,
    modified_at                 DATETIME(6)     NULL,
    modified_by                 VARCHAR(150)    NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_delivery_workflow_trip (trip_id),
    INDEX idx_delivery_workflow_trip_status (trip_id, entity_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 3. trip_delivery_return_line
--    Individual stock items returned; many per workflow.
-- --------------------------------------------------------
CREATE TABLE trip_delivery_return_line (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    workflow_id         BIGINT          NOT NULL,
    product_name        VARCHAR(500)    NOT NULL,
    quantity            DECIMAL(19, 2)  NOT NULL DEFAULT 0.00,
    reason              VARCHAR(500)    NULL,
    recorded_by_role    VARCHAR(50)     NULL COMMENT 'DRIVER | CUSTOMER | RECEIVER',

    -- Audit
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(150)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(150)    NULL,

    PRIMARY KEY (id),
    INDEX idx_return_line_workflow (workflow_id, entity_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
