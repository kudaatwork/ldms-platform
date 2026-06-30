-- Offer / acceptance lifecycle on supplier ↔ transporter links.
-- Existing rows default to ACCEPTED so already-linked transporters keep behaving as active contracts.
ALTER TABLE organization_contracted_transporters
    ADD COLUMN link_status VARCHAR(30) NOT NULL DEFAULT 'ACCEPTED' AFTER transporter_id,
    ADD COLUMN requested_by VARCHAR(150) NULL AFTER link_status,
    ADD COLUMN responded_at DATETIME(6) NULL AFTER requested_by,
    ADD COLUMN responded_by VARCHAR(150) NULL AFTER responded_at;
