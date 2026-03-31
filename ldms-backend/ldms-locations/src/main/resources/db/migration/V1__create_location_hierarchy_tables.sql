CREATE TABLE IF NOT EXISTS location_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(100) NULL,
    location_type VARCHAR(50) NOT NULL,
    parent_id BIGINT NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    timezone VARCHAR(100) NULL,
    postal_code VARCHAR(30) NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(100) NULL,
    CONSTRAINT fk_location_node_parent FOREIGN KEY (parent_id) REFERENCES location_node(id)
);

CREATE INDEX idx_location_node_parent ON location_node(parent_id);
CREATE INDEX idx_location_node_type ON location_node(location_type);
CREATE INDEX idx_location_node_status ON location_node(entity_status);

CREATE TABLE IF NOT EXISTS location_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_node_id BIGINT NOT NULL,
    alias VARCHAR(150) NOT NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(100) NULL,
    CONSTRAINT fk_location_alias_node FOREIGN KEY (location_node_id) REFERENCES location_node(id)
);

CREATE INDEX idx_location_alias_node ON location_alias(location_node_id);
