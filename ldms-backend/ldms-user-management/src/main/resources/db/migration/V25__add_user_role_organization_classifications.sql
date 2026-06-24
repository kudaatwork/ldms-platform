-- Organization classifications applicable to each user role.
-- A role with no rows in this table is considered platform-only (admin portal).
-- A role with rows is applicable to the listed organization classifications.
CREATE TABLE user_role_organization_classifications (
    user_role_id BIGINT NOT NULL,
    organization_classification VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_role_id, organization_classification),
    CONSTRAINT fk_uroc_user_role FOREIGN KEY (user_role_id) REFERENCES user_role (id) ON DELETE CASCADE
);

CREATE INDEX idx_uroc_classification ON user_role_organization_classifications (organization_classification);
