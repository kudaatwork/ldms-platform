-- IFRS 21: store transaction currency amounts with functional currency conversion locked at order date

ALTER TABLE purchase_order
    ADD COLUMN functional_currency_code VARCHAR(3) NULL
        COMMENT 'Entity functional (books) currency' AFTER currency,
    ADD COLUMN exchange_rate_snapshot_id BIGINT NULL
        COMMENT 'Immutable spot rate snapshot at order date' AFTER functional_currency_code,
    ADD COLUMN exchange_rate_used DECIMAL(19, 8) NULL
        COMMENT 'Transaction to functional rate applied at order date' AFTER exchange_rate_snapshot_id,
    ADD COLUMN subtotal_functional DECIMAL(19, 4) NULL
        COMMENT 'Subtotal in functional currency' AFTER total_amount,
    ADD COLUMN tax_amount_functional DECIMAL(19, 4) NULL
        COMMENT 'Tax in functional currency' AFTER subtotal_functional,
    ADD COLUMN total_amount_functional DECIMAL(19, 4) NULL
        COMMENT 'Total in functional currency' AFTER tax_amount_functional;

ALTER TABLE purchase_order_line
    ADD COLUMN unit_price_functional DECIMAL(19, 4) NULL
        COMMENT 'Unit price in functional currency at order date rate' AFTER total_price,
    ADD COLUMN total_price_functional DECIMAL(19, 4) NULL
        COMMENT 'Line total in functional currency at order date rate' AFTER unit_price_functional,
    ADD COLUMN exchange_rate_snapshot_id BIGINT NULL
        COMMENT 'Rate snapshot for this line (same as header when foreign currency)' AFTER total_price_functional;
