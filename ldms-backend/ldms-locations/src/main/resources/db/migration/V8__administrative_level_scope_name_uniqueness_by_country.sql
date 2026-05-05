-- Administrative level names (e.g. Province, ADM1) repeat per country; uniqueness must be per country, not global.
ALTER TABLE administrative_level
    DROP INDEX uk_administrative_level_name;

CREATE INDEX idx_administrative_level_country_name ON administrative_level (country_id, name);
