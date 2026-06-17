-- Platform-portal organisation users eligible to allocate fleet (driver + vehicle) to shipments.
ALTER TABLE `user`
    ADD COLUMN shipment_fleet_allocator BOOLEAN NOT NULL DEFAULT FALSE AFTER procurement_approver;

CREATE INDEX idx_user_shipment_fleet_allocator ON `user` (shipment_fleet_allocator, organization_id, entity_status);
