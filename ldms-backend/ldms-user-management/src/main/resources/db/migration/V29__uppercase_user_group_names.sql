-- Uppercase all user group names for consistent storage and display.
UPDATE user_group
SET name = UPPER(TRIM(name)),
    updated_at = NOW(6)
WHERE name IS NOT NULL
  AND name <> UPPER(TRIM(name));
