-- Organisation-scoped user groups (e.g. per-org "Administrator") vs platform-wide groups (organization_id NULL).
ALTER TABLE user_group
    ADD COLUMN organization_id BIGINT NULL AFTER description;

CREATE INDEX idx_user_group_organization_id ON user_group (organization_id, entity_status);

CREATE UNIQUE INDEX uk_user_group_org_name ON user_group (organization_id, name);
