-- Organisation class label for workspace user groups (SUPPLIER, TRANSPORT_COMPANY, etc.).
ALTER TABLE user_group
    ADD COLUMN organization_classification VARCHAR(50) NULL AFTER organization_id;

CREATE INDEX idx_user_group_org_classification ON user_group (organization_id, organization_classification, entity_status);
