-- IFRS 21: realized exchange gain/loss on payment settlement vs invoice origination rate

ALTER TABLE payment
    ADD COLUMN amount_functional_at_origination DECIMAL(19, 4) NOT NULL DEFAULT 0.0000
        COMMENT 'Functional amount at invoice/transaction-date spot rate' AFTER amount_base,
    ADD COLUMN realized_fx_gain_loss DECIMAL(19, 4) NOT NULL DEFAULT 0.0000
        COMMENT 'Settlement functional minus origination functional (gain positive)' AFTER amount_functional_at_origination,
    ADD COLUMN invoice_exchange_rate_snapshot_id BIGINT NULL
        COMMENT 'Exchange rate snapshot locked at invoice origination' AFTER exchange_rate_snapshot_id;

UPDATE payment
SET amount_functional_at_origination = amount_base,
    realized_fx_gain_loss = 0.0000
WHERE amount_functional_at_origination = 0.0000;
