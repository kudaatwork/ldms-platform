-- Duplex mode: one legal entity can buy from some partners and sell to others under one organisation record.
ALTER TABLE organization
    ADD COLUMN duplex_mode BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'When true, org may act as customer and supplier; primary classification unchanged'
        AFTER organization_classification;
