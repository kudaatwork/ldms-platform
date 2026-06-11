#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$REPO_ROOT/ldms-backend"

move_tree() {
  local module="$1"
  local old_rel="$2"
  local new_rel="$3"
  local module_dir="$BACKEND/$module"

  for src_root in "$module_dir/src/main/java" "$module_dir/src/test/java"; do
    local old_dir="$src_root/$old_rel"
    local new_dir="$src_root/$new_rel"
    if [[ -d "$old_dir" ]]; then
      mkdir -p "$(dirname "$new_dir")"
      mv "$old_dir" "$new_dir"
      echo "Moved $old_dir -> $new_dir"
    fi
  done

  # validation -> validator
  for src_root in "$module_dir/src/main/java" "$module_dir/src/test/java"; do
    local validator_parent="$src_root/$new_rel/business"
    if [[ -d "$validator_parent/validation" ]]; then
      mv "$validator_parent/validation" "$validator_parent/validator"
      echo "Renamed validation -> validator in $validator_parent"
    fi
  done
}

move_tree "ldms-fleet-management" \
  "projectlx/co/zw/fleetmanagement" \
  "projectlx/fleet/management"

move_tree "ldms-inventory-management" \
  "projectlx/co/zw/inventorymanagementservice" \
  "projectlx/inventory/management"

move_tree "ldms-billing-payments" \
  "projectlx/co/zw/billingpayments" \
  "projectlx/billing/payments"

move_tree "ldms-shipment-management" \
  "projectlx/co/zw/shipmentmanagement" \
  "projectlx/shipment/management"

move_tree "ldms-trip-tracking" \
  "projectlx/co/zw/triptracking" \
  "projectlx/trip/tracking"

# Inventory application class rename
INV_MODULE="$BACKEND/ldms-inventory-management"
INV_APP="$INV_MODULE/src/main/java/projectlx/inventory/management/InventoryManagementServiceApplication.java"
if [[ -f "$INV_APP" ]]; then
  mv "$INV_APP" "${INV_APP/InventoryManagementServiceApplication/InventoryManagementApplication}"
  sed -i '' 's/InventoryManagementServiceApplication/InventoryManagementApplication/g' \
    "$INV_MODULE/src/main/java/projectlx/inventory/management/InventoryManagementApplication.java" \
    "$INV_MODULE/pom.xml"
fi
INV_TEST="$INV_MODULE/src/test/java/projectlx/inventory/management/InventoryManagementServiceApplicationTests.java"
if [[ -f "$INV_TEST" ]]; then
  mv "$INV_TEST" "${INV_TEST/InventoryManagementServiceApplicationTests/InventoryManagementApplicationTests}"
  sed -i '' 's/InventoryManagementServiceApplication/InventoryManagementApplication/g' \
    "$INV_MODULE/src/test/java/projectlx/inventory/management/InventoryManagementApplicationTests.java"
fi

# pom.xml mainClass
update_pom() {
  local module="$1"
  local main_class="$2"
  local pom="$BACKEND/$module/pom.xml"
  [[ -f "$pom" ]] && sed -i '' -E "s|<mainClass>[^<]+</mainClass>|<mainClass>${main_class}</mainClass>|g" "$pom"
}

update_pom "ldms-fleet-management" "projectlx.fleet.management.FleetManagementApplication"
update_pom "ldms-inventory-management" "projectlx.inventory.management.InventoryManagementApplication"
update_pom "ldms-billing-payments" "projectlx.billing.payments.BillingPaymentsApplication"
update_pom "ldms-shipment-management" "projectlx.shipment.management.ShipmentManagementApplication"
update_pom "ldms-trip-tracking" "projectlx.trip.tracking.TripTrackingApplication"

# Remove empty co/zw dirs
find "$BACKEND"/ldms-{fleet-management,inventory-management,billing-payments,shipment-management,trip-tracking} \
  -type d -empty -delete 2>/dev/null || true

echo "Directory fix complete."
