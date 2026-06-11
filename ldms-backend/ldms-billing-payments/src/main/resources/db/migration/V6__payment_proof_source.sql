-- Distinguish system-recorded vs externally uploaded payment proofs (both equally valid; metadata always required).

ALTER TABLE payment
    ADD COLUMN proof_source VARCHAR(30) NULL COMMENT 'SYSTEM_GENERATED or EXTERNAL_UPLOAD' AFTER proof_document_id;

UPDATE payment
SET proof_source = CASE
    WHEN proof_document_id IS NOT NULL THEN 'EXTERNAL_UPLOAD'
    ELSE 'SYSTEM_GENERATED'
END
WHERE proof_source IS NULL;
