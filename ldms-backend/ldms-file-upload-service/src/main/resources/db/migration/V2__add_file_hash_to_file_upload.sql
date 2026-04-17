ALTER TABLE file_upload
    ADD COLUMN file_hash VARCHAR(128) NULL AFTER file_type;
