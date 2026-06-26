-- Organisation payments (with proof) can now activate a subscription package, not just top up
-- the wallet. Tag each deposit with its purpose and the target package so LX approval can branch.

ALTER TABLE wallet_deposit
    ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'WALLET_TOPUP' AFTER status,
    ADD COLUMN subscription_package_id BIGINT NULL AFTER purpose;

-- Existing rows are all wallet top-ups.
UPDATE wallet_deposit SET purpose = 'WALLET_TOPUP' WHERE purpose IS NULL;
