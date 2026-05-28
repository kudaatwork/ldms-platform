-- Pre-assigned reviewers for signup organisations (least-load algorithm).
ALTER TABLE organization
    ADD COLUMN assigned_stage1_approver_user_id BIGINT NULL AFTER assigned_account_manager_user_id,
    ADD COLUMN assigned_stage1_approver_username VARCHAR(150) NULL AFTER assigned_stage1_approver_user_id,
    ADD COLUMN assigned_stage2_approver_user_id BIGINT NULL AFTER assigned_stage1_approver_username,
    ADD COLUMN assigned_stage2_approver_username VARCHAR(150) NULL AFTER assigned_stage2_approver_user_id;

CREATE INDEX idx_org_assigned_stage1_approver ON organization (assigned_stage1_approver_user_id);
CREATE INDEX idx_org_assigned_stage2_approver ON organization (assigned_stage2_approver_user_id);
