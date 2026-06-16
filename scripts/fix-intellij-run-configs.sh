#!/usr/bin/env bash
# Fixes stale IntelliJ run configurations after microservice package migrations.
# Run with IntelliJ CLOSED so workspace.xml is not overwritten on save.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKSPACE="$ROOT/.idea/workspace.xml"

if [[ ! -f "$WORKSPACE" ]]; then
  echo "No $WORKSPACE found; nothing to fix."
  exit 0
fi

python3 - "$WORKSPACE" <<'PY'
from pathlib import Path
import re
import sys

workspace = Path(sys.argv[1])
text = workspace.read_text()

replacements = {
    "projectlx.co.zw.billingpayments.BillingPaymentsApplication": "projectlx.billing.payments.BillingPaymentsApplication",
    "projectlx.co.zw.fleetmanagement.FleetManagementApplication": "projectlx.fleet.management.FleetManagementApplication",
    "projectlx.co.zw.shipmentmanagement.ShipmentManagementApplication": "projectlx.shipment.management.ShipmentManagementApplication",
    "projectlx.co.zw.triptracking.TripTrackingApplication": "projectlx.trip.tracking.TripTrackingApplication",
}
for old, new in replacements.items():
    text = text.replace(old, new)

migrated = {
    "BillingPaymentsApplication",
    "FleetManagementApplication",
    "ShipmentManagementApplication",
    "TripTrackingApplication",
    "InventoryManagementApplication",
    "UserManagementApplication",
}
for name in migrated:
    text = re.sub(
        rf'\n\s*<configuration name="{re.escape(name)}" type="SpringBootApplicationConfigurationType"[\s\S]*?</configuration>',
        '',
        text,
        count=1,
    )

text = re.sub(
    r'\n\s*<configuration name="BillingPaymentsApplication \(1\)"[\s\S]*?</configuration>',
    '',
    text,
)

workspace.write_text(text)

import xml.etree.ElementTree as ET
try:
    ET.parse(workspace)
except ET.ParseError as exc:
    raise SystemExit(f"workspace.xml is invalid after fix: {exc}")
PY

echo "Updated $WORKSPACE"
echo "Shared run configs live in .idea/runConfigurations/ (FleetManagementApplication, etc.)."
echo "Reopen IntelliJ and use those run configurations."
