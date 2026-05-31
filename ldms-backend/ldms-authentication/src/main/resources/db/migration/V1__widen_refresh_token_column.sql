-- JWT refresh tokens exceed the default VARCHAR(255) Hibernate created on older schemas.
ALTER TABLE token
    MODIFY COLUMN token VARCHAR(2048) NOT NULL;
