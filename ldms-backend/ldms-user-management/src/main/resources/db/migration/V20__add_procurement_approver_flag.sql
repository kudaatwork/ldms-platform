-- Platform-portal organisation users eligible to approve procurement workflow stages.
ALTER TABLE `user`
    ADD COLUMN procurement_approver BOOLEAN NOT NULL DEFAULT FALSE AFTER operational_issue_handler;

CREATE INDEX idx_user_procurement_approver ON `user` (procurement_approver, organization_id, entity_status);
