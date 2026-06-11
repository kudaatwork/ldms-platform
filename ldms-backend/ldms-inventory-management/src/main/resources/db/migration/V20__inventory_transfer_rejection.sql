-- Inventory transfer rejection tracking (approver denies a REQUESTED transfer)

ALTER TABLE inventory_transfer
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER reference,
    ADD COLUMN rejected_by_user_id BIGINT NULL AFTER rejection_reason,
    ADD COLUMN rejected_at DATETIME(6) NULL AFTER rejected_by_user_id;
