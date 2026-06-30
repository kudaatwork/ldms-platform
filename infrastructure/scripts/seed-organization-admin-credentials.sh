#!/usr/bin/env bash
# Provisions organisation contact-person admins and normalises dev login credentials.
# Usage: ./infrastructure/scripts/seed-organization-admin-credentials.sh
# Password (all org admins): Kaydizzy098#@!

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GATEWAY="${LDMS_GATEWAY:-http://127.0.0.1:8091}"
MYSQL_CONTAINER="${LDMS_MYSQL_CONTAINER:-ldms-mysql}"
MYSQL_USER="${MYSQL_USER:-developer}"
MYSQL_PASS="${MYSQL_DEVELOPER_PASSWORD:-Password098()!}"
DEV_PASSWORD='Kaydizzy098#@!'
BCRYPT_HASH='$2y$10$93CXOoBG9KKyU66ptp/jNu/DgWwRYycZYaZM1EqKTsznkWRZy8zaK'

mysql_exec() {
  docker exec "$MYSQL_CONTAINER" mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -N -B -e "$1" 2>/dev/null
}

echo "==> LDMS organisation admin credential seed"
echo "    Gateway: $GATEWAY"
echo "    Password: $DEV_PASSWORD"
echo

# Provision missing contact persons via system API (best-effort).
while IFS=$'\t' read -r org_id name classification contact_email contact_user_id; do
  [[ -z "${org_id:-}" ]] && continue
  if [[ -z "${contact_user_id}" || "${contact_user_id}" == "NULL" ]]; then
    echo "    Provisioning contact person for org $org_id ($name)…"
    curl -sf -X POST \
      "$GATEWAY/ldms-organization-management/v1/system/organization/${org_id}/provision-contact-person" \
      -H 'Accept-Language: en' >/dev/null 2>&1 || \
      echo "    WARN: provision-contact-person failed for org $org_id (is org-management up?)"
  fi
done < <(mysql_exec "
SELECT o.id, o.name, o.organization_classification, IFNULL(o.contact_person_email,''), IFNULL(o.contact_person_user_id,'')
FROM ldms_organization_management.organization o
WHERE o.entity_status = 'ACTIVE' AND o.kyc_status = 'APPROVED'
ORDER BY o.id;
")

echo "==> Normalising admin users in MySQL…"

mysql_exec "
UPDATE ldms_organization_management.organization o
INNER JOIN ldms_user_management.user u ON u.id = COALESCE(
  NULLIF(o.contact_person_user_id, 0),
  (SELECT MIN(u2.id) FROM ldms_user_management.user u2
   WHERE u2.organization_id = o.id AND u2.entity_status = 'ACTIVE')
)
SET o.contact_person_user_id = u.id,
    o.is_verified = 1,
    o.modified_at = NOW(6),
    o.modified_by = 'DEV_SEED'
WHERE o.entity_status = 'ACTIVE' AND o.kyc_status = 'APPROVED';

UPDATE ldms_user_management.user u
INNER JOIN ldms_organization_management.organization o ON o.id = u.organization_id
SET u.username = CONCAT('admin.org', o.id),
    u.email = COALESCE(NULLIF(TRIM(o.contact_person_email), ''), u.email),
    u.first_name = COALESCE(NULLIF(TRIM(o.contact_person_first_name), ''), u.first_name, 'Admin'),
    u.last_name = COALESCE(NULLIF(TRIM(o.contact_person_last_name), ''), u.last_name, o.name),
    u.must_change_credentials = 0,
    u.email_verified = 1,
    u.user_type_id = (
      SELECT id FROM ldms_user_management.user_type
      WHERE user_type_name = 'System Administrator' AND entity_status = 'ACTIVE' LIMIT 1
    ),
    u.user_group_id = CASE o.organization_classification
      WHEN 'SUPPLIER' THEN 16 WHEN 'CUSTOMER' THEN 17 WHEN 'TRANSPORT_COMPANY' THEN 20
      WHEN 'CLEARING_AGENT' THEN 21 WHEN 'SERVICE_STATION' THEN 18 WHEN 'ROADSIDE_SUPPORT_SERVICE' THEN 19
      WHEN 'GOVERNMENT_AGENCY' THEN 22 ELSE u.user_group_id END,
    u.updated_at = NOW(6)
WHERE o.entity_status = 'ACTIVE' AND o.kyc_status = 'APPROVED' AND u.id = o.contact_person_user_id;

UPDATE ldms_user_management.user_password up
INNER JOIN ldms_user_management.user u ON u.id = up.user_id
INNER JOIN ldms_organization_management.organization o ON o.id = u.organization_id
SET up.password = '${BCRYPT_HASH}',
    up.is_password_expired = 0,
    up.expiry_date = DATE_ADD(NOW(6), INTERVAL 365 DAY),
    up.updated_at = NOW(6)
WHERE o.entity_status = 'ACTIVE' AND o.kyc_status = 'APPROVED'
  AND u.id = o.contact_person_user_id AND up.entity_status = 'ACTIVE';

INSERT INTO ldms_user_management.user_password (user_id, password, is_password_expired, expiry_date, entity_status, created_at, updated_at)
SELECT u.id, '${BCRYPT_HASH}', 0, DATE_ADD(NOW(6), INTERVAL 365 DAY), 'ACTIVE', NOW(6), NOW(6)
FROM ldms_user_management.user u
INNER JOIN ldms_organization_management.organization o ON o.id = u.organization_id
LEFT JOIN ldms_user_management.user_password up ON up.user_id = u.id AND up.entity_status = 'ACTIVE'
WHERE o.entity_status = 'ACTIVE' AND o.kyc_status = 'APPROVED'
  AND u.id = o.contact_person_user_id AND up.id IS NULL;
" >/dev/null

echo
echo "==> Organisation admin credentials (platform portal: http://localhost:4201)"
echo
printf "%-4s %-32s %-18s %-28s %s\n" "ID" "Organisation" "Classification" "Username" "Password"
printf "%s\n" "---- -------------------------------- ------------------ ---------------------------- ----------------"

while IFS=$'\t' read -r org_id name classification username; do
  [[ -z "${org_id:-}" ]] && continue
  printf "%-4s %-32s %-18s %-28s %s\n" "$org_id" "${name:0:32}" "$classification" "$username" "$DEV_PASSWORD"
done < <(mysql_exec "
SELECT o.id, o.name, o.organization_classification, u.username
FROM ldms_organization_management.organization o
INNER JOIN ldms_user_management.user u ON u.id = o.contact_person_user_id
WHERE o.entity_status = 'ACTIVE' AND o.kyc_status = 'APPROVED'
ORDER BY o.id;
")

echo
echo "Done. Sign in at the platform portal with any row above."
