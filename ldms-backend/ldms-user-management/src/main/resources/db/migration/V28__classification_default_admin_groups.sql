-- Classification default admin groups: one shared system Administrator group per organisation
-- classification (organization_id IS NULL, organization_classification = <CLASS>), replacing the
-- per-organisation Administrator duplicates. Admin users of every org in a classification inherit
-- their admin roles from this single shared default. Adds an is_locked flag (platform-admin toggled).
--
-- Assumes the ldms_organization_management schema is reachable on the same MySQL server (as V27).

-- 1. Lock flag on user_group (defaults to locked = inherited roles read-only for org admins).
--    Idempotent: a prior failed run may have added the column before step 4 aborted.
SET @is_locked_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_group'
      AND COLUMN_NAME = 'is_locked'
);
SET @add_is_locked = IF(
    @is_locked_exists = 0,
    'ALTER TABLE user_group ADD COLUMN is_locked TINYINT(1) NOT NULL DEFAULT 1',
    'SELECT 1'
);
PREPARE stmt_add_is_locked FROM @add_is_locked;
EXECUTE stmt_add_is_locked;
DEALLOCATE PREPARE stmt_add_is_locked;

-- 2. Create the shared classification default Administrator groups (skeleton rows) if absent.
INSERT INTO user_group
    (name, description, organization_id, organization_classification, is_system_group, is_locked, entity_status, created_at, updated_at)
SELECT 'Administrator',
       CONCAT('Default administrators inherited by all ', c.classification, ' organisations'),
       NULL, c.classification, 1, 1, 'ACTIVE', NOW(6), NOW(6)
FROM (
    SELECT 'SUPPLIER' AS classification
    UNION ALL SELECT 'CUSTOMER'
    UNION ALL SELECT 'SERVICE_STATION'
    UNION ALL SELECT 'ROADSIDE_SUPPORT_SERVICE'
    UNION ALL SELECT 'TRANSPORT_COMPANY'
    UNION ALL SELECT 'CLEARING_AGENT'
    UNION ALL SELECT 'GOVERNMENT_AGENCY'
) c
WHERE NOT EXISTS (
    SELECT 1 FROM user_group g
    WHERE g.organization_id IS NULL
      AND LOWER(g.name) = 'administrator'
      AND UPPER(g.organization_classification) = c.classification
      AND g.entity_status <> 'DELETED'
);

-- 3. Link classification-filtered roles (and lock them as defaults) onto each classification default.
--    A role applies to a classification when it has a row in user_role_organization_classifications.
INSERT IGNORE INTO user_group_user_role (user_group_id, user_role_id)
SELECT g.id, uroc.user_role_id
FROM user_group g
JOIN user_role_organization_classifications uroc
     ON UPPER(uroc.organization_classification) = UPPER(g.organization_classification)
JOIN user_role ur ON ur.id = uroc.user_role_id AND ur.entity_status = 'ACTIVE'
WHERE g.organization_id IS NULL
  AND LOWER(g.name) = 'administrator'
  AND UPPER(g.organization_classification) <> 'ADMIN_PORTAL'
  AND g.entity_status <> 'DELETED';

INSERT IGNORE INTO user_group_default_roles (user_group_id, user_role_id)
SELECT ugur.user_group_id, ugur.user_role_id
FROM user_group_user_role ugur
JOIN user_group g ON g.id = ugur.user_group_id
WHERE g.organization_id IS NULL
  AND LOWER(g.name) = 'administrator'
  AND UPPER(g.organization_classification) <> 'ADMIN_PORTAL'
  AND g.entity_status <> 'DELETED';

-- 4. Re-point members of per-organisation Administrator groups onto the matching classification default
--    (resolved from the user's organisation classification).
UPDATE user u
JOIN user_group oag
     ON oag.id = u.user_group_id
    AND oag.organization_id IS NOT NULL
    AND LOWER(oag.name) = 'administrator'
JOIN ldms_organization_management.organization o ON o.id = u.organization_id
JOIN user_group def
     ON def.organization_id IS NULL
    AND LOWER(def.name) = 'administrator'
    AND UPPER(def.organization_classification) COLLATE utf8mb4_unicode_ci
        = UPPER(o.organization_classification)
    AND def.entity_status <> 'DELETED'
SET u.user_group_id = def.id,
    u.updated_at = NOW(6)
WHERE u.entity_status <> 'DELETED';

-- 4b. Fallback: any remaining members still on a per-org Administrator group (e.g. org without a
--     classification) move to the platform Administrator so no one is left on a deleted group.
UPDATE user u
JOIN user_group oag
     ON oag.id = u.user_group_id
    AND oag.organization_id IS NOT NULL
    AND LOWER(oag.name) = 'administrator'
JOIN user_group plat
     ON plat.organization_id IS NULL
    AND LOWER(plat.name) = 'administrator'
    AND (plat.organization_classification = 'ADMIN_PORTAL' OR plat.organization_classification IS NULL)
    AND plat.entity_status <> 'DELETED'
SET u.user_group_id = plat.id,
    u.updated_at = NOW(6)
WHERE u.entity_status <> 'DELETED';

-- 5. Retire the per-organisation Administrator duplicates.
UPDATE user_group
SET entity_status = 'DELETED',
    updated_at = NOW(6)
WHERE organization_id IS NOT NULL
  AND LOWER(name) = 'administrator'
  AND entity_status <> 'DELETED';
