CREATE TABLE IF NOT EXISTS geo_coordinates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    latitude DECIMAL(10,6) NOT NULL,
    longitude DECIMAL(10,6) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL
);

CREATE TABLE IF NOT EXISTS language (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    iso_code VARCHAR(5) NOT NULL,
    native_name VARCHAR(255) NULL,
    is_default BIT(1) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT uk_language_name UNIQUE (name),
    CONSTRAINT uk_language_iso_code UNIQUE (iso_code)
);

CREATE TABLE IF NOT EXISTS country (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    iso_alpha2_code VARCHAR(2) NOT NULL,
    iso_alpha3_code VARCHAR(3) NOT NULL,
    dial_code VARCHAR(255) NOT NULL,
    timezone VARCHAR(255) NOT NULL,
    currency_code VARCHAR(255) NULL,
    geo_coordinates_id BIGINT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT uk_country_name UNIQUE (name),
    CONSTRAINT uk_country_iso_alpha2_code UNIQUE (iso_alpha2_code),
    CONSTRAINT uk_country_iso_alpha3_code UNIQUE (iso_alpha3_code),
    CONSTRAINT fk_country_geo_coordinates FOREIGN KEY (geo_coordinates_id) REFERENCES geo_coordinates(id)
);

CREATE TABLE IF NOT EXISTS administrative_level (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NULL,
    level INT NOT NULL,
    description VARCHAR(255) NULL,
    country_id BIGINT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT uk_administrative_level_name UNIQUE (name),
    CONSTRAINT fk_administrative_level_country FOREIGN KEY (country_id) REFERENCES country(id)
);

CREATE TABLE IF NOT EXISTS province (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10) NULL,
    country_id BIGINT NOT NULL,
    administrative_level_id BIGINT NULL,
    geo_coordinates_id BIGINT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT fk_province_country FOREIGN KEY (country_id) REFERENCES country(id),
    CONSTRAINT fk_province_administrative_level FOREIGN KEY (administrative_level_id) REFERENCES administrative_level(id),
    CONSTRAINT fk_province_geo_coordinates FOREIGN KEY (geo_coordinates_id) REFERENCES geo_coordinates(id)
);

CREATE TABLE IF NOT EXISTS district (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10) NULL,
    province_id BIGINT NOT NULL,
    administrative_level_id BIGINT NULL,
    geo_coordinates_id BIGINT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT fk_district_province FOREIGN KEY (province_id) REFERENCES province(id),
    CONSTRAINT fk_district_administrative_level FOREIGN KEY (administrative_level_id) REFERENCES administrative_level(id),
    CONSTRAINT fk_district_geo_coordinates FOREIGN KEY (geo_coordinates_id) REFERENCES geo_coordinates(id)
);

CREATE TABLE IF NOT EXISTS suburb (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10) NULL,
    district_id BIGINT NOT NULL,
    geo_coordinates_id BIGINT NULL,
    postal_code VARCHAR(255) NULL,
    administrative_level_id BIGINT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT fk_suburb_district FOREIGN KEY (district_id) REFERENCES district(id),
    CONSTRAINT fk_suburb_geo_coordinates FOREIGN KEY (geo_coordinates_id) REFERENCES geo_coordinates(id),
    CONSTRAINT fk_suburb_administrative_level FOREIGN KEY (administrative_level_id) REFERENCES administrative_level(id)
);

CREATE TABLE IF NOT EXISTS address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255) NULL,
    postal_code VARCHAR(255) NULL,
    suburb_id BIGINT NULL,
    geo_coordinates_id BIGINT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT fk_address_suburb FOREIGN KEY (suburb_id) REFERENCES suburb(id),
    CONSTRAINT fk_address_geo_coordinates FOREIGN KEY (geo_coordinates_id) REFERENCES geo_coordinates(id)
);

CREATE TABLE IF NOT EXISTS localized_name (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    `value` VARCHAR(255) NOT NULL,
    language_id BIGINT NOT NULL,
    country_id BIGINT NULL,
    province_id BIGINT NULL,
    district_id BIGINT NULL,
    suburb_id BIGINT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    entity_status VARCHAR(50) NULL,
    CONSTRAINT fk_localized_name_language FOREIGN KEY (language_id) REFERENCES language(id),
    CONSTRAINT fk_localized_name_country FOREIGN KEY (country_id) REFERENCES country(id),
    CONSTRAINT fk_localized_name_province FOREIGN KEY (province_id) REFERENCES province(id),
    CONSTRAINT fk_localized_name_district FOREIGN KEY (district_id) REFERENCES district(id),
    CONSTRAINT fk_localized_name_suburb FOREIGN KEY (suburb_id) REFERENCES suburb(id)
);
