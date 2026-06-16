-- Two-level branch hierarchy: Branch (level 1) -> Sub-branch / Depot (level 2)

ALTER TABLE organization_branch
    ADD COLUMN parent_branch_id BIGINT NULL AFTER organization_id,
    ADD COLUMN branch_level VARCHAR(50) NOT NULL DEFAULT 'BRANCH' COMMENT 'BRANCH | SUB_BRANCH',
    ADD COLUMN is_depot BOOLEAN NOT NULL DEFAULT FALSE AFTER branch_level;

ALTER TABLE organization_branch
    ADD CONSTRAINT fk_branch_parent FOREIGN KEY (parent_branch_id)
        REFERENCES organization_branch (id) ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE INDEX idx_branch_parent ON organization_branch (parent_branch_id);
CREATE INDEX idx_branch_level ON organization_branch (branch_level);

-- Existing rows are top-level branches
UPDATE organization_branch
SET branch_level = 'BRANCH',
    parent_branch_id = NULL
WHERE branch_level IS NULL OR branch_level = '';
