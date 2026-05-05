ALTER TABLE address
    ADD COLUMN external_source VARCHAR(50) NULL AFTER settlement_type,
    ADD COLUMN external_place_id VARCHAR(255) NULL AFTER external_source,
    ADD COLUMN formatted_address VARCHAR(500) NULL AFTER external_place_id;

CREATE INDEX idx_address_external_place_id ON address(external_place_id);
