-- Roadside support directory: fuel stations, mechanics, and roadside services along corridors
CREATE TABLE roadside_provider (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    provider_type       VARCHAR(50)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(500)    NULL,
    phone               VARCHAR(40)     NULL,
    services_offered    VARCHAR(500)    NULL,
    latitude            DECIMAL(10, 7)  NOT NULL,
    longitude           DECIMAL(10, 7)  NOT NULL,
    address_label       VARCHAR(255)    NULL,
    open_24_hours       TINYINT(1)      NOT NULL DEFAULT 0,
    verified            TINYINT(1)      NOT NULL DEFAULT 1,
    entity_status       VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6)     NOT NULL,
    created_by          VARCHAR(100)    NOT NULL,
    modified_at         DATETIME(6)     NULL,
    modified_by         VARCHAR(100)    NULL,
    PRIMARY KEY (id),
    INDEX idx_roadside_provider_type (provider_type, entity_status),
    INDEX idx_roadside_provider_geo (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roadside_provider
    (provider_type, name, description, phone, services_offered, latitude, longitude, address_label, open_24_hours, verified, entity_status, created_at, created_by)
VALUES
    ('FUEL_STATION', 'Beitbridge Border Fuel Stop', '24h diesel and AdBlue for cross-border trucks', '+263 77 123 4501', 'Diesel, AdBlue, tyre pressure', -22.2167000, 30.0000000, 'Beitbridge, Matabeleland South', 1, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('FUEL_STATION', 'Masvingo Highway Services', 'Full-service truck stop on A4', '+263 77 234 5602', 'Diesel, lubricants, convenience', -20.0633000, 30.8278000, 'Masvingo', 0, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('FUEL_STATION', 'Harare West Truck Plaza', 'Major refuel hub west of Harare', '+263 77 345 6703', 'Diesel, parking, showers', -17.8500000, 30.9500000, 'Harare West', 1, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('MECHANIC', 'Gweru Roadside Mechanics', 'Heavy-duty breakdown and tyre repair', '+263 77 456 7804', 'Engine, tyres, electrical', -19.4500000, 29.8167000, 'Gweru', 0, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('MECHANIC', 'Plumtree Mobile Workshop', 'Border-route mobile mechanic unit', '+263 77 567 8905', 'Breakdown, welding, hydraulics', -20.4833000, 27.8167000, 'Plumtree', 0, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('ROADSIDE_SUPPORT', 'Chirundu Recovery & Tow', 'Recovery and escort for abnormal loads', '+263 77 678 9016', 'Tow, recovery, escort', -16.0333000, 28.8500000, 'Chirundu', 1, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('FUEL_STATION', 'Bulawayo N1 Truck Stop', 'N1 corridor diesel and rest', '+263 77 789 0127', 'Diesel, rest, food', -20.1500000, 28.5833000, 'Bulawayo', 1, 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('MECHANIC', 'Mutare Eastern Workshop', 'Eastern route breakdown support', '+263 77 890 1238', 'Mechanical, refrigeration units', -18.9700000, 32.6700000, 'Mutare', 0, 1, 'ACTIVE', NOW(6), 'SYSTEM');
