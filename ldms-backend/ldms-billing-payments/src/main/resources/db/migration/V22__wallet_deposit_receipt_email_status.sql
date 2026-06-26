-- Track whether the receipt email was dispatched when a wallet deposit is approved, so admins can
-- see delivery status in the deposit detail view. Best-effort: reflects publishing to the
-- notification queue (SENT / NO_EMAIL / FAILED), not inbox delivery.

ALTER TABLE wallet_deposit
    ADD COLUMN receipt_email_status VARCHAR(20) NULL AFTER rejection_reason,
    ADD COLUMN receipt_email_address VARCHAR(255) NULL AFTER receipt_email_status,
    ADD COLUMN receipt_email_at TIMESTAMP NULL AFTER receipt_email_address;
