-- Currency master data and country base-currency settings

CREATE TABLE currency (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    code                VARCHAR(3)      NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    symbol              VARCHAR(10)     NULL,
    decimal_places      INT             NOT NULL DEFAULT 2,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(255)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_currency_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE country_currency_setting (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    country_id          BIGINT          NOT NULL,
    country_name        VARCHAR(200)    NOT NULL,
    country_iso_alpha2  VARCHAR(2)      NOT NULL,
    base_currency_code  VARCHAR(3)      NOT NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(255)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_country_currency_country_id (country_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exchange_rate (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    from_currency_code  VARCHAR(3)      NOT NULL,
    to_currency_code    VARCHAR(3)      NOT NULL,
    rate                DECIMAL(19,8)   NOT NULL,
    effective_from      DATETIME(6)     NOT NULL,
    effective_to        DATETIME(6)     NULL,
    source              VARCHAR(50)     NOT NULL DEFAULT 'MANUAL',
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(255)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    KEY idx_exchange_rate_pair_effective (from_currency_code, to_currency_code, effective_from),
    KEY idx_exchange_rate_effective_to (effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Immutable snapshot locked at transaction time (accounting principle)
CREATE TABLE exchange_rate_snapshot (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    exchange_rate_id    BIGINT          NULL,
    from_currency_code  VARCHAR(3)      NOT NULL,
    to_currency_code    VARCHAR(3)      NOT NULL,
    rate                DECIMAL(19,8)   NOT NULL,
    effective_at        DATETIME(6)     NOT NULL,
    source              VARCHAR(50)     NOT NULL,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(255)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    KEY idx_exchange_rate_snapshot_pair (from_currency_code, to_currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO currency (code, name, symbol, decimal_places, entity_status, created_at, created_by)
VALUES
    ('USD', 'US Dollar', '$', 2, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ZWL', 'Zimbabwe Dollar', 'Z$', 2, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ZAR', 'South African Rand', 'R', 2, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('EUR', 'Euro', '€', 2, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('GBP', 'British Pound', '£', 2, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('BWP', 'Botswana Pula', 'P', 2, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ZMW', 'Zambian Kwacha', 'ZK', 2, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('MZN', 'Mozambican Metical', 'MT', 2, 'ACTIVE', NOW(6), 'SYSTEM');
