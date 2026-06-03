-- Platform-wide default and per-organisation override for KYC approval stage count.
CREATE TABLE platform_kyc_policy (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    default_required_approval_stages INT NOT NULL DEFAULT 2,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6) NULL,
    modified_by VARCHAR(150) NULL
);

INSERT INTO platform_kyc_policy (
    default_required_approval_stages,
    entity_status,
    created_at,
    created_by
) VALUES (
    2,
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    'SYSTEM'
);

ALTER TABLE organization
    ADD COLUMN kyc_required_approval_stages INT NULL AFTER assigned_stage2_approver_username;
