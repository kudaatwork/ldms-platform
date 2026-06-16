-- ============================================================
-- V2: IoT route plan + live position for trip corridor tracking
-- Database: ldms_trip_tracking
-- ============================================================

CREATE TABLE IF NOT EXISTS trip_route_plan
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id                 BIGINT         NOT NULL,
    organization_id         BIGINT         NOT NULL,
    origin_label            VARCHAR(200)   NULL,
    destination_label       VARCHAR(200)   NULL,
    origin_latitude         DECIMAL(10, 7) NOT NULL,
    origin_longitude        DECIMAL(10, 7) NOT NULL,
    destination_latitude    DECIMAL(10, 7) NOT NULL,
    destination_longitude   DECIMAL(10, 7) NOT NULL,
    waypoints_json          JSON           NOT NULL COMMENT 'Ordered corridor checkpoints [{label,lat,lng}]',
    total_distance_km       DECIMAL(10, 2) NULL,
    simulation_active       TINYINT(1)     NOT NULL DEFAULT 0,
    current_segment_index   INT            NOT NULL DEFAULT 0,
    segment_progress_pct    DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    overall_progress_pct    DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    current_latitude        DECIMAL(10, 7) NULL,
    current_longitude       DECIMAL(10, 7) NULL,
    current_speed_kmh       DECIMAL(6, 2)  NULL,
    current_heading_deg     DECIMAL(6, 2)  NULL,
    entity_status           VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME(6)    NOT NULL,
    created_by              VARCHAR(150)   NOT NULL,
    modified_at             DATETIME(6)    NULL,
    modified_by             VARCHAR(150)   NULL,

    CONSTRAINT uk_trip_route_plan_trip UNIQUE (trip_id),
    CONSTRAINT fk_trip_route_plan_trip FOREIGN KEY (trip_id) REFERENCES trip (id),
    INDEX idx_trip_route_plan_org (organization_id, entity_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
