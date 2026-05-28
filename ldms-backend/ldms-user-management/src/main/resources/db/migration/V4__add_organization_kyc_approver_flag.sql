-- Organization KYC approvers are admin-portal users (no organisation assignment).
ALTER TABLE `user`
    ADD COLUMN organization_kyc_approver BOOLEAN NOT NULL DEFAULT FALSE AFTER branch_id;

CREATE INDEX idx_user_org_kyc_approver ON `user` (organization_kyc_approver, entity_status);
