-- Add rejection_reason to wallet_deposit for admin reject-with-reason workflow.

ALTER TABLE wallet_deposit
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER payment_method;
