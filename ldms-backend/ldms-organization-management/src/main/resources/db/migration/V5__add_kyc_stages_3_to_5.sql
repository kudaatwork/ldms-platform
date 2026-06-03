-- Stages 3–5: assigned reviewers and review audit columns for multi-approver KYC pipelines.
ALTER TABLE organization
    ADD COLUMN assigned_stage3_approver_user_id BIGINT NULL AFTER assigned_stage2_approver_username,
    ADD COLUMN assigned_stage3_approver_username VARCHAR(150) NULL AFTER assigned_stage3_approver_user_id,
    ADD COLUMN assigned_stage4_approver_user_id BIGINT NULL AFTER assigned_stage3_approver_username,
    ADD COLUMN assigned_stage4_approver_username VARCHAR(150) NULL AFTER assigned_stage4_approver_user_id,
    ADD COLUMN assigned_stage5_approver_user_id BIGINT NULL AFTER assigned_stage4_approver_username,
    ADD COLUMN assigned_stage5_approver_username VARCHAR(150) NULL AFTER assigned_stage5_approver_user_id,
    ADD COLUMN stage3_reviewed_by VARCHAR(150) NULL AFTER stage2_reviewed_at,
    ADD COLUMN stage3_reviewed_at DATETIME(6) NULL AFTER stage3_reviewed_by,
    ADD COLUMN stage4_reviewed_by VARCHAR(150) NULL AFTER stage3_reviewed_at,
    ADD COLUMN stage4_reviewed_at DATETIME(6) NULL AFTER stage4_reviewed_by,
    ADD COLUMN stage5_reviewed_by VARCHAR(150) NULL AFTER stage4_reviewed_at,
    ADD COLUMN stage5_reviewed_at DATETIME(6) NULL AFTER stage5_reviewed_by;

CREATE INDEX idx_org_assigned_stage3_approver ON organization (assigned_stage3_approver_user_id);
CREATE INDEX idx_org_assigned_stage4_approver ON organization (assigned_stage4_approver_user_id);
CREATE INDEX idx_org_assigned_stage5_approver ON organization (assigned_stage5_approver_user_id);
