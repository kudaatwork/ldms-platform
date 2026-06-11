-- ========================================================================
-- Flyway Migration V1: Base Tables (CORRECTED to match actual entities)
-- ========================================================================
-- Purpose: Core master data tables matching your JPA entity models
-- Reference: Generated from your uploaded model classes
-- ========================================================================

-- ========================================================================
-- Table: product_category
-- Entity: ProductCategory.java
-- ========================================================================
CREATE TABLE product_category (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  name VARCHAR(200) NOT NULL,
                                  description VARCHAR(500),

    -- Audit fields
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                  PRIMARY KEY (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Product categories';

CREATE INDEX idx_product_category_name ON product_category(name);

-- ========================================================================
-- Table: product_sub_category
-- Entity: ProductSubCategory.java
-- ========================================================================
CREATE TABLE product_sub_category (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      name VARCHAR(200) NOT NULL,
                                      description VARCHAR(500),
                                      category_id BIGINT NOT NULL,

    -- Audit fields
                                      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                      updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                      entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                      PRIMARY KEY (id),
                                      CONSTRAINT fk_subcategory_category FOREIGN KEY (category_id)
                                          REFERENCES product_category(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Product sub-categories';

CREATE INDEX idx_product_subcategory_category ON product_sub_category(category_id);

-- ========================================================================
-- Table: warehouse_location
-- Entity: WarehouseLocation.java
-- ========================================================================
CREATE TABLE warehouse_location (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    name VARCHAR(200) NOT NULL,
                                    description VARCHAR(500) NOT NULL,
                                    location_id VARCHAR(100) NOT NULL COMMENT 'FK to Location Management Service',
                                    supplier_id BIGINT COMMENT 'Warehouse owner (FK to Org Service)',

    -- Audit fields
                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                    PRIMARY KEY (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Warehouse locations';

CREATE INDEX idx_warehouse_location_supplier ON warehouse_location(supplier_id);
CREATE INDEX idx_warehouse_location_location_id ON warehouse_location(location_id);

-- ========================================================================
-- Table: product
-- Entity: Product.java
-- Note: unit_of_measure is stored as VARCHAR (enum), NOT a foreign key
-- ========================================================================
CREATE TABLE product (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         supplier_id BIGINT NOT NULL COMMENT 'FK to Org Service',
                         name VARCHAR(300) NOT NULL,
                         description TEXT,
                         price DECIMAL(19, 4) NOT NULL,

    -- UnitOfMeasure is an ENUM stored as VARCHAR
                         unit_of_measure VARCHAR(50) NOT NULL COMMENT 'ENUM: EACH, BOX, PACK, KILOGRAM, etc.',

                         product_code VARCHAR(100) NOT NULL,
                         category_id BIGINT NOT NULL,
                         subcategory_id BIGINT,
                         manufacturer VARCHAR(200),
                         image_id BIGINT COMMENT 'FK to File Upload Service',

    -- Audit fields
                         created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                         expires_at DATE,
                         entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                         PRIMARY KEY (id),
                         CONSTRAINT fk_product_category FOREIGN KEY (category_id)
                             REFERENCES product_category(id) ON DELETE RESTRICT,
                         CONSTRAINT fk_product_subcategory FOREIGN KEY (subcategory_id)
                             REFERENCES product_sub_category(id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Product master catalog';

CREATE UNIQUE INDEX ux_product_product_code ON product(product_code);
CREATE INDEX idx_product_category ON product(category_id);
CREATE INDEX idx_product_subcategory ON product(subcategory_id);
CREATE INDEX idx_product_supplier ON product(supplier_id);
CREATE INDEX idx_product_name ON product(name);

-- ========================================================================
-- Table: product_document
-- Entity: ProductDocument.java
-- ========================================================================
CREATE TABLE product_document (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  product_id BIGINT NOT NULL,
                                  document_name VARCHAR(200) NOT NULL,
                                  document_type VARCHAR(50),
                                  file_path VARCHAR(500),
                                  file_id BIGINT COMMENT 'FK to File Upload Service',

    -- Audit fields
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_product_doc_product FOREIGN KEY (product_id)
                                      REFERENCES product(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Product documents (specs, certs, images)';

CREATE INDEX idx_product_document_product ON product_document(product_id);

-- ========================================================================
-- END OF V1 MIGRATION
-- ========================================================================