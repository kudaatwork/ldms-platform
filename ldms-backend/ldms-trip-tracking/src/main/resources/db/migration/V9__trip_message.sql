-- Driver ⇄ receiver in-app chat, scoped per trip.
CREATE TABLE IF NOT EXISTS trip_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    sender_user_id  BIGINT NOT NULL,
    sender_role     VARCHAR(20) NOT NULL,
    sender_name     VARCHAR(150),
    body            VARCHAR(2000) NOT NULL,
    read_at         DATETIME(6) NULL,
    entity_status   VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(150) NOT NULL,
    modified_at     DATETIME(6) NULL,
    modified_by     VARCHAR(150) NULL,
    CONSTRAINT fk_trip_message_trip FOREIGN KEY (trip_id) REFERENCES trip (id),
    INDEX idx_trip_message_trip (trip_id, entity_status),
    INDEX idx_trip_message_created (trip_id, created_at)
);
