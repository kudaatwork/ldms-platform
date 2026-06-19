-- V16: Add operational mode settings to the organization table.
--
-- standalone_mode        : org runs solo logistics; counterparty is a CRM/trading-partner record only (no LDMS platform user).
-- inventory_management_enabled : full stock management via LDMS Inventory module (default true).
-- cross_docking_enabled  : org operates cross-dock logistics; stock is not stored (mutually exclusive with full inventory).
-- inventory_data_source  : how inventory data is sourced (INTERNAL | EXTERNAL_API | MANUAL_ACK).

ALTER TABLE organization
    ADD COLUMN standalone_mode               BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN inventory_management_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN cross_docking_enabled         BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN inventory_data_source         VARCHAR(50)  NOT NULL DEFAULT 'INTERNAL';
