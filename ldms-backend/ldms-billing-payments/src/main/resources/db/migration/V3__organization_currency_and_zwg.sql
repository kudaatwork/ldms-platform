-- Organisation functional currency (multicurrency books) + Zimbabwe Gold (ZWG)

INSERT IGNORE INTO currency (code, name, symbol, decimal_places, entity_status, created_at, created_by)
VALUES ('ZWG', 'Zimbabwe Gold', 'ZWG', 2, 'ACTIVE', NOW(6), 'SYSTEM');

CREATE TABLE organization_currency_setting (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id             BIGINT          NOT NULL,
    organization_name           VARCHAR(200)    NOT NULL,
    country_id                  BIGINT          NULL,
    country_iso_alpha2          VARCHAR(2)      NULL,
    functional_currency_code    VARCHAR(3)      NOT NULL,
    entity_status               VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at                  DATETIME(6)     NOT NULL,
    created_by                  VARCHAR(255)    NOT NULL,
    modified_at                 DATETIME(6)     NULL,
    modified_by                 VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_org_currency_organization_id (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1 USD = 26.8 ZWG (Zimbabwe multicurrency reference rate)
INSERT INTO exchange_rate (
    from_currency_code, to_currency_code, rate, effective_from, effective_to, source,
    entity_status, created_at, created_by
)
VALUES ('USD', 'ZWG', 26.80000000, NOW(6), NULL, 'MANUAL', 'ACTIVE', NOW(6), 'SYSTEM');
