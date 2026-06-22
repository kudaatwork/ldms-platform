ALTER TABLE `user`
    ADD COLUMN billing_approver BOOLEAN NOT NULL DEFAULT FALSE AFTER shipment_fleet_allocator;

CREATE INDEX idx_user_billing_approver ON `user` (billing_approver, organization_id, entity_status);
