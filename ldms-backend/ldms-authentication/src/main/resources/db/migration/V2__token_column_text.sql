-- Refresh JWTs for users with large role sets can exceed VARCHAR(2048).
ALTER TABLE token
    MODIFY COLUMN token TEXT NOT NULL;
