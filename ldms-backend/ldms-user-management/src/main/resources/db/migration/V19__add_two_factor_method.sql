-- Discriminator for SMS vs authenticator-app 2FA at login and in My Account.
ALTER TABLE user_security
    ADD COLUMN two_factor_method VARCHAR(50) NULL;

UPDATE user_security
SET two_factor_method = 'SMS'
WHERE is_two_factor_enabled = 1
  AND two_factor_method IS NULL;
