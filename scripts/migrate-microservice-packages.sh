#!/usr/bin/env bash
# Migrates LDMS microservice packages to projectlx.<domain>.<subdomain> convention.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$REPO_ROOT/ldms-backend"

migrate_service() {
  local module="$1"
  local old_pkg="$2"
  local new_pkg="$3"
  local old_path="${old_pkg//./\/}"
  local new_path="${new_pkg//./\/}"
  local module_dir="$BACKEND/$module"

  echo "=== Migrating $module: $old_pkg -> $new_pkg ==="

  for src_root in "$module_dir/src/main/java" "$module_dir/src/test/java"; do
    local old_dir="$src_root/$old_path"
    local new_dir="$src_root/$new_path"
    if [[ -d "$old_dir" ]]; then
      mkdir -p "$(dirname "$new_dir")"
      git mv "$old_dir" "$new_dir" 2>/dev/null || mv "$old_dir" "$new_dir"
    fi
  done

  # validation -> validator (user-management convention)
  for src_root in "$module_dir/src/main/java" "$module_dir/src/test/java"; do
    local validator_parent="$src_root/$new_path/business"
    if [[ -d "$validator_parent/validation" ]]; then
      git mv "$validator_parent/validation" "$validator_parent/validator" 2>/dev/null \
        || mv "$validator_parent/validation" "$validator_parent/validator"
    fi
  done

  # Replace package references in all text files under the module
  while IFS= read -r -d '' file; do
    if grep -q "$old_pkg\|business\.validation" "$file" 2>/dev/null; then
      sed -i '' \
        -e "s/${old_pkg//./\\.}/${new_pkg//./\\.}/g" \
        -e 's/business\.validation/business.validator/g' \
        "$file"
    fi
  done < <(find "$module_dir" -type f \( \
    -name '*.java' -o -name '*.xml' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' \
    \) -print0)
}

# Fleet management
migrate_service "ldms-fleet-management" \
  "projectlx.co.zw.fleetmanagement" \
  "projectlx.fleet.management"

# Inventory management
migrate_service "ldms-inventory-management" \
  "projectlx.co.zw.inventorymanagementservice" \
  "projectlx.inventory.management"

INV_MODULE="$BACKEND/ldms-inventory-management"
INV_APP_DIR="$INV_MODULE/src/main/java/projectlx/inventory/management"
INV_TEST_DIR="$INV_MODULE/src/test/java/projectlx/inventory/management"
if [[ -f "$INV_APP_DIR/InventoryManagementServiceApplication.java" ]]; then
  git mv "$INV_APP_DIR/InventoryManagementServiceApplication.java" \
    "$INV_APP_DIR/InventoryManagementApplication.java" 2>/dev/null \
    || mv "$INV_APP_DIR/InventoryManagementServiceApplication.java" \
    "$INV_APP_DIR/InventoryManagementApplication.java"
  sed -i '' \
    -e 's/InventoryManagementServiceApplication/InventoryManagementApplication/g' \
    "$INV_APP_DIR/InventoryManagementApplication.java" \
    "$INV_MODULE/pom.xml"
  if [[ -f "$INV_TEST_DIR/InventoryManagementServiceApplicationTests.java" ]]; then
    git mv "$INV_TEST_DIR/InventoryManagementServiceApplicationTests.java" \
      "$INV_TEST_DIR/InventoryManagementApplicationTests.java" 2>/dev/null \
      || mv "$INV_TEST_DIR/InventoryManagementServiceApplicationTests.java" \
      "$INV_TEST_DIR/InventoryManagementApplicationTests.java"
    sed -i '' \
      -e 's/InventoryManagementServiceApplication/InventoryManagementApplication/g' \
      "$INV_TEST_DIR/InventoryManagementApplicationTests.java"
  fi
fi

# Billing payments
migrate_service "ldms-billing-payments" \
  "projectlx.co.zw.billingpayments" \
  "projectlx.billing.payments"

# Shipment management
migrate_service "ldms-shipment-management" \
  "projectlx.co.zw.shipmentmanagement" \
  "projectlx.shipment.management"

# Trip tracking
migrate_service "ldms-trip-tracking" \
  "projectlx.co.zw.triptracking" \
  "projectlx.trip.tracking"

# Update pom.xml mainClass for each module
declare -A MAIN_CLASSES=(
  ["ldms-fleet-management"]="projectlx.fleet.management.FleetManagementApplication"
  ["ldms-inventory-management"]="projectlx.inventory.management.InventoryManagementApplication"
  ["ldms-billing-payments"]="projectlx.billing.payments.BillingPaymentsApplication"
  ["ldms-shipment-management"]="projectlx.shipment.management.ShipmentManagementApplication"
  ["ldms-trip-tracking"]="projectlx.trip.tracking.TripTrackingApplication"
)

for module in "${!MAIN_CLASSES[@]}"; do
  pom="$BACKEND/$module/pom.xml"
  main_class="${MAIN_CLASSES[$module]}"
  if [[ -f "$pom" ]]; then
    sed -i '' -E "s|<mainClass>[^<]+</mainClass>|<mainClass>${main_class}</mainClass>|g" "$pom"
  fi
done

# Clean up empty co/zw directories
find "$BACKEND"/ldms-{fleet-management,inventory-management,billing-payments,shipment-management,trip-tracking} \
  -type d -empty -delete 2>/dev/null || true

echo "=== Migration complete ==="
