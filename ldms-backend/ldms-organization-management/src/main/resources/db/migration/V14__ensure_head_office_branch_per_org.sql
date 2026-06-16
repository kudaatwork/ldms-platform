-- Ensure every active organisation has a head-office branch for warehouse / transit linkage

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code, is_head_office, is_active,
    branch_level, is_depot, entity_status, created_at, created_by
)
SELECT o.id,
       CONCAT(o.name, ' - Head Office'),
       CONCAT('HO-', o.id),
       TRUE,
       TRUE,
       'BRANCH',
       FALSE,
       'ACTIVE',
       NOW(6),
       'SYSTEM'
FROM organization o
WHERE o.entity_status <> 'DELETED'
  AND NOT EXISTS (
    SELECT 1
    FROM organization_branch b
    WHERE b.organization_id = o.id
      AND b.is_head_office = TRUE
      AND b.entity_status <> 'DELETED'
);
