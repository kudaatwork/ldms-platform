-- Backfill branch_id on existing physical warehouses using each org's head-office branch id
-- stored in a temporary mapping from organisation DB is not possible in SQL; assign via placeholder
-- and rely on system backfill endpoint. For dev: branch_id stays NULL until backfill runs.
-- Create virtual in-transit warehouse shell rows per supplier org (completed at runtime if missing).

UPDATE warehouse_location
SET is_virtual = FALSE
WHERE is_virtual IS NULL;
