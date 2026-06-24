-- Populate role organization classifications based on RoleClassificationPolicy.
-- Platform-only roles (ADMIN, READ_ONLY, KYC_*) remain unmapped (empty classifications).
-- All other roles map to ALL organization classifications.

-- Clear stale mappings for deleted roles
DELETE uroc FROM user_role_organization_classifications uroc
LEFT JOIN user_role ur ON ur.id = uroc.user_role_id
WHERE ur.id IS NULL OR ur.entity_status = 'DELETED';

-- For each non-platform role, insert all 7 classifications
INSERT IGNORE INTO user_role_organization_classifications (user_role_id, organization_classification)
SELECT ur.id, classification FROM user_role ur
CROSS JOIN (
    SELECT 'SUPPLIER' as classification
    UNION ALL SELECT 'CUSTOMER'
    UNION ALL SELECT 'SERVICE_STATION'
    UNION ALL SELECT 'ROADSIDE_SUPPORT_SERVICE'
    UNION ALL SELECT 'TRANSPORT_COMPANY'
    UNION ALL SELECT 'CLEARING_AGENT'
    UNION ALL SELECT 'GOVERNMENT_AGENCY'
) classifications
WHERE ur.entity_status = 'ACTIVE'
  AND ur.role NOT IN (
    'ADMIN',
    'READ_ONLY',
    'KYC_STAGE1',
    'KYC_STAGE2',
    'KYC_STAGE3',
    'KYC_STAGE4',
    'KYC_STAGE5',
    'CHURN_OUT_AUDIT_LOGS'
  );

-- Remove mappings for platform-only roles (they should have no classifications)
DELETE uroc FROM user_role_organization_classifications uroc
INNER JOIN user_role ur ON ur.id = uroc.user_role_id
WHERE ur.role IN (
    'ADMIN',
    'READ_ONLY',
    'KYC_STAGE1',
    'KYC_STAGE2',
    'KYC_STAGE3',
    'KYC_STAGE4',
    'KYC_STAGE5',
    'CHURN_OUT_AUDIT_LOGS'
  );
