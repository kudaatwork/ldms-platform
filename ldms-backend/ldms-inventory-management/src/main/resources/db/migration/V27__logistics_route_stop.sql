-- Route stops for logistics context objects (transfers, POs, SOs).
-- stop_type follows RouteStopContextType: ORIGIN, EN_ROUTE_DEPOT, DESTINATION.
-- context_type follows RouteStopContextType: INVENTORY_TRANSFER, PURCHASE_ORDER, SALES_ORDER.

CREATE TABLE logistics_route_stop (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id      BIGINT         NOT NULL,
    context_type         VARCHAR(50)    NOT NULL COMMENT 'INVENTORY_TRANSFER | PURCHASE_ORDER | SALES_ORDER',
    context_id           BIGINT         NOT NULL,
    stop_sequence        INT            NOT NULL,
    stop_type            VARCHAR(50)    NOT NULL COMMENT 'ORIGIN | EN_ROUTE_DEPOT | DESTINATION',
    warehouse_location_id BIGINT        NULL,
    branch_id            BIGINT         NULL,
    location_label       VARCHAR(200)   NULL,
    entity_status        VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at           DATETIME(6)    NULL,
    created_by           VARCHAR(100)   NULL,
    modified_at          DATETIME(6)    NULL,
    modified_by          VARCHAR(100)   NULL,

    INDEX idx_route_stop_context (context_type, context_id, stop_sequence),
    INDEX idx_route_stop_org     (organization_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
