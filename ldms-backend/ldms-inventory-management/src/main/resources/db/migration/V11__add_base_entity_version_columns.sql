-- Add optimistic-lock version column required by BaseEntity on utility and sales-order tables.

ALTER TABLE idempotency_key
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER entity_status;

ALTER TABLE sales_order
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER entity_status;

ALTER TABLE sales_order_line
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER entity_status;
