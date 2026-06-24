-- Make organisation administrators members of their own organisation's Administrator group,
-- backfill classification on Administrator groups, and treat the admin portal as a classification.
--
-- Assumes the organisation-management schema (ldms_organization_management) is reachable on the
-- same database server (the shared LDMS MySQL instance). New provisioning also sets these values,
-- so this migration only repairs pre-existing rows. Organisations that still lack an Administrator
-- group are provisioned at runtime (OrganizationWorkspaceProvisioner) with classification-filtered roles.

-- 1. Every Administrator group is a system (default, locked) group.
UPDATE user_group
SET is_system_group = 1,
    updated_at = NOW(6)
WHERE LOWER(name) = 'administrator'
  AND entity_status <> 'DELETED';

-- 2. Backfill the organisation classification on organisation-scoped Administrator groups.
UPDATE user_group g
JOIN ldms_organization_management.organization o ON o.id = g.organization_id
SET g.organization_classification = o.organization_classification,
    g.updated_at = NOW(6)
WHERE LOWER(g.name) = 'administrator'
  AND g.organization_id IS NOT NULL
  AND (g.organization_classification IS NULL OR g.organization_classification = '');

-- 3. The platform-wide Administrator group represents the admin portal classification.
UPDATE user_group
SET organization_classification = 'ADMIN_PORTAL',
    is_system_group = 1,
    updated_at = NOW(6)
WHERE LOWER(name) = 'administrator'
  AND organization_id IS NULL
  AND entity_status <> 'DELETED';

-- 4. Lock each system Administrator group's current roles in as its defaults (drives the locked UI).
INSERT IGNORE INTO user_group_default_roles (user_group_id, user_role_id)
SELECT ugur.user_group_id, ugur.user_role_id
FROM user_group_user_role ugur
JOIN user_group g ON g.id = ugur.user_group_id
WHERE g.is_system_group = 1
  AND LOWER(g.name) = 'administrator'
  AND g.entity_status <> 'DELETED'
  AND NOT EXISTS (
        SELECT 1 FROM user_group_default_roles d
        WHERE d.user_group_id = ugur.user_group_id AND d.user_role_id = ugur.user_role_id);

-- 5. Move organisation-scoped users off the shared platform Administrator group onto their own
--    organisation's Administrator group. Platform operators (organization_id IS NULL) stay put.
UPDATE user u
JOIN user_group g
     ON g.organization_id = u.organization_id
    AND LOWER(g.name) = 'administrator'
    AND g.entity_status <> 'DELETED'
JOIN user_group cur ON cur.id = u.user_group_id
SET u.user_group_id = g.id,
    u.updated_at = NOW(6)
WHERE u.organization_id IS NOT NULL
  AND u.entity_status = 'ACTIVE'
  AND u.user_group_id <> g.id
  AND cur.organization_id IS NULL;
