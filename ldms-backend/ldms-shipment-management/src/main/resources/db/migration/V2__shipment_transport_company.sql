-- Assign transport company before fleet (driver/vehicle) allocation.

ALTER TABLE shipment
    ADD COLUMN transport_company_organization_id BIGINT NULL AFTER fleet_asset_id,
    ADD COLUMN transport_company_name            VARCHAR(200) NULL AFTER transport_company_organization_id;

CREATE INDEX idx_shipment_transport_company_id
    ON shipment (transport_company_organization_id, entity_status);
