-- V18: How counterparties are engaged when standalone_mode is false.
-- RECORD_ONLY   : CRM/trading-partner records — counterparty does not log in.
-- PLATFORM_ORG  : register or link full platform organisations.

ALTER TABLE organization
    ADD COLUMN counterparty_engagement_mode VARCHAR(50) NOT NULL DEFAULT 'PLATFORM_ORG';
