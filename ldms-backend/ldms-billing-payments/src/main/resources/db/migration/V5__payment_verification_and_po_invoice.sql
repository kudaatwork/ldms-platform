-- Payment proof upload, verification workflow, and PO-sourced invoices

ALTER TABLE payment
    ADD COLUMN payment_reference_number VARCHAR(100) NULL AFTER payment_reference,
    ADD COLUMN proof_document_id BIGINT NULL AFTER payment_method,
    ADD COLUMN verified_at DATETIME(6) NULL AFTER status,
    ADD COLUMN verified_by VARCHAR(255) NULL AFTER verified_at,
    ADD COLUMN gateway_provider VARCHAR(50) NULL COMMENT 'PAYPAL, MASTERCARD, etc.' AFTER verified_by;

-- Existing rows treated as verified legacy payments
UPDATE payment SET status = 'COMPLETED' WHERE status = 'COMPLETED' AND verified_at IS NULL;
UPDATE payment SET verified_at = created_at, verified_by = created_by WHERE status = 'COMPLETED' AND verified_at IS NULL;
