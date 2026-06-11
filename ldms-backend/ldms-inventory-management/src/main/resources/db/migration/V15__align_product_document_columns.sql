-- Align product_document with ProductDocument entity (JPA schema validation).

ALTER TABLE product_document
    ADD COLUMN name VARCHAR(200) NULL AFTER product_id,
    ADD COLUMN description VARCHAR(500) NULL AFTER name,
    ADD COLUMN document_id VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'External file storage reference' AFTER description,
    ADD COLUMN expires_at DATE NULL AFTER updated_at;

UPDATE product_document
SET name = document_name
WHERE name IS NULL
  AND document_name IS NOT NULL;

UPDATE product_document
SET document_id = CAST(file_id AS CHAR)
WHERE document_id = ''
  AND file_id IS NOT NULL;
