-- Operational issue handler eligibility (admin-portal users, like organisation KYC approvers)
-- and support ticket assignment to those handlers.

ALTER TABLE `user`
    ADD COLUMN operational_issue_handler BOOLEAN NOT NULL DEFAULT FALSE AFTER organization_kyc_approver;

CREATE INDEX idx_user_operational_issue_handler ON `user` (operational_issue_handler, entity_status);

ALTER TABLE support_ticket
    ADD COLUMN assigned_handler_user_id BIGINT NULL AFTER organization_name,
    ADD COLUMN assigned_handler_username VARCHAR(150) NULL AFTER assigned_handler_user_id;

CREATE INDEX idx_support_ticket_assigned_handler ON support_ticket (assigned_handler_user_id);
