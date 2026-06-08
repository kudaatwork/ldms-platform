-- MFA 2FA challenge tokens persist TokenType.MFA_CHALLENGE (13 chars).
-- Older Hibernate schemas used a very short token_type column (e.g. VARCHAR(6) for BEARER only),
-- which causes: Data truncated for column 'token_type' at row 1
ALTER TABLE token
    MODIFY COLUMN token_type VARCHAR(50) NOT NULL;
