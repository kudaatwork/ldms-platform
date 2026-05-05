ALTER TABLE location_node
    ADD COLUMN district_id BIGINT NULL AFTER parent_id,
    ADD COLUMN suburb_id BIGINT NULL AFTER district_id,
    ADD CONSTRAINT fk_location_node_district FOREIGN KEY (district_id) REFERENCES district(id),
    ADD CONSTRAINT fk_location_node_suburb FOREIGN KEY (suburb_id) REFERENCES suburb(id);

CREATE INDEX idx_location_node_district ON location_node(district_id);
CREATE INDEX idx_location_node_suburb ON location_node(suburb_id);

ALTER TABLE suburb
    ADD COLUMN city_location_node_id BIGINT NULL AFTER district_id,
    ADD CONSTRAINT fk_suburb_city_location_node FOREIGN KEY (city_location_node_id) REFERENCES location_node(id);

CREATE INDEX idx_suburb_city_location_node ON suburb(city_location_node_id);

ALTER TABLE address
    ADD COLUMN settlement_type VARCHAR(50) NULL AFTER postal_code,
    ADD COLUMN village_location_node_id BIGINT NULL AFTER suburb_id,
    ADD CONSTRAINT fk_address_village_location_node FOREIGN KEY (village_location_node_id) REFERENCES location_node(id);

CREATE INDEX idx_address_settlement_type ON address(settlement_type);
CREATE INDEX idx_address_village_location_node ON address(village_location_node_id);

UPDATE address
SET settlement_type = 'SUBURB'
WHERE settlement_type IS NULL AND suburb_id IS NOT NULL;
