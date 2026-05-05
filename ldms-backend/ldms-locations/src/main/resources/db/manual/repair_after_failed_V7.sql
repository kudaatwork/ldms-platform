-- =============================================================================
-- After V7 failed once, Flyway refuses to start until the failure is cleared.
--
-- With profile `dev`, the service runs Flyway.repair() before migrate automatically
-- (see FlywayDevRepairStrategyConfig) — you usually do not need this script locally.
--
-- Use this script for shared / non-dev databases, or if you prefer a manual fix:
-- run against `ldms_location_management`, then restart so Flyway can re-apply V7.
-- =============================================================================

-- 1) Remove the failed migration row (Flyway validate error: "failed migration to version 7")
DELETE FROM flyway_schema_history
WHERE version = '7'
  AND success = 0;

-- 2) If V7 partially ran (tables/columns exist), clean up so a full V7 re-run succeeds.
--    Uncomment and run ONLY if restart fails with duplicate key / duplicate column errors.

-- SET FOREIGN_KEY_CHECKS = 0;
-- DROP TABLE IF EXISTS village;
-- DROP TABLE IF EXISTS city;
-- SET FOREIGN_KEY_CHECKS = 1;
--
-- ALTER TABLE suburb DROP COLUMN city_id;
-- ALTER TABLE address DROP COLUMN village_id;
-- ALTER TABLE address DROP COLUMN city_id;
