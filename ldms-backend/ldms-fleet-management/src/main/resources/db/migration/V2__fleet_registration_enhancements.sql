-- ================================================================
-- Service: ldms-fleet-management
-- Version: V2
-- Purpose: Fleet registration enhancements — contract scope,
--          job reference, and registration status columns.
--          New assets start as PENDING_COMPLIANCE and are
--          activated after required compliance documents are uploaded.
-- ================================================================

ALTER TABLE fleet_asset
    ADD COLUMN contract_scope      VARCHAR(50)  NOT NULL DEFAULT 'LONG_TERM'
        COMMENT 'LONG_TERM (default), JOB — scope of contracted arrangement',
    ADD COLUMN job_reference       VARCHAR(100) NULL
        COMMENT 'Required when contract_scope = JOB; identifies the specific job',
    ADD COLUMN registration_status VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'PENDING_COMPLIANCE (new assets) → ACTIVE (after documents submitted)';

-- Backfill: all existing rows are already operationally active
UPDATE fleet_asset
SET registration_status = 'ACTIVE'
WHERE registration_status IS NULL OR registration_status = '';

ALTER TABLE fleet_asset
    ADD KEY idx_fleet_asset_reg_status (organization_id, registration_status, entity_status);
