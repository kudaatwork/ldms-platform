-- Head-office branches from V14 were created without contact metadata.
-- Copy organisation inbox / phone / regions / hours onto the head-office row when blank.

UPDATE organization_branch b
INNER JOIN organization o ON o.id = b.organization_id
SET
    b.email = COALESCE(
        NULLIF(TRIM(b.email), ''),
        NULLIF(TRIM(o.email), ''),
        NULLIF(TRIM(o.contact_person_email), '')
    ),
    b.phone_number = COALESCE(
        NULLIF(TRIM(b.phone_number), ''),
        NULLIF(TRIM(o.phone_number), ''),
        NULLIF(TRIM(o.contact_person_phone_number), '')
    ),
    b.region = COALESCE(
        NULLIF(TRIM(b.region), ''),
        NULLIF(TRIM(SUBSTRING_INDEX(o.regions_served, ',', 1)), '')
    ),
    b.business_hours = COALESCE(
        NULLIF(TRIM(b.business_hours), ''),
        NULLIF(TRIM(o.business_hours), '')
    ),
    b.modified_at = NOW(6),
    b.modified_by = 'SYSTEM'
WHERE b.is_head_office = TRUE
  AND b.entity_status <> 'DELETED'
  AND o.entity_status <> 'DELETED';
