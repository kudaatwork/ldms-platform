-- ================================================================
-- Service: ldms-fleet-management
-- Version: V3
-- Purpose: Long-term contracted vehicle engagement dates.
-- ================================================================

ALTER TABLE fleet_asset
    ADD COLUMN contract_start_date DATE NULL
        COMMENT 'Vehicle engagement start when ownership=CONTRACTED and contract_scope=LONG_TERM',
    ADD COLUMN contract_end_date DATE NULL
        COMMENT 'Vehicle engagement end; must fall within transporter partner contract window';
