-- Token table already uses VARCHAR for token_type (no ENUM constraint).
-- This migration documents the addition of MFA_CHALLENGE as a valid TokenType value
-- used by the two-factor authentication challenge flow.
-- No DDL change is required; the column already accepts any VARCHAR(50) value.
SELECT 1;
