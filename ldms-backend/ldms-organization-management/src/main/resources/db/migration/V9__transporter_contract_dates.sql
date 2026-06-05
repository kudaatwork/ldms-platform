-- Contract metadata on supplier ↔ transporter links
ALTER TABLE organization_contracted_transporters
    ADD COLUMN contract_start_date DATE NULL AFTER transporter_id,
    ADD COLUMN contract_end_date DATE NULL AFTER contract_start_date,
    ADD COLUMN linked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER contract_end_date,
    ADD COLUMN entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' AFTER linked_at,
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER entity_status,
    ADD COLUMN created_by VARCHAR(150) NULL AFTER created_at,
    ADD COLUMN modified_at DATETIME(6) NULL AFTER created_by,
    ADD COLUMN modified_by VARCHAR(150) NULL AFTER modified_at;

UPDATE organization_contracted_transporters
SET contract_start_date = DATE(linked_at)
WHERE contract_start_date IS NULL;
