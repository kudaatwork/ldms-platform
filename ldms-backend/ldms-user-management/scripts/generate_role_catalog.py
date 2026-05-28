#!/usr/bin/env python3
"""Regenerate ldms/role-catalog.properties from all *Roles.java enums under ldms-backend."""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
BACKEND = REPO_ROOT / "ldms-backend"
OUT = (
    BACKEND
    / "ldms-user-management/src/main/resources/ldms/role-catalog.properties"
)
ROLE_PATTERN = re.compile(r'\s+(\w+)\("([^"]+)"\s*,\s*"([^"]*)"\)\s*,?')


def main() -> int:
    roles: dict[str, str] = {}
    for path in sorted(BACKEND.rglob("*Roles.java")):
        text = path.read_text(encoding="utf-8")
        for _, role, description in ROLE_PATTERN.findall(text):
            key = role.strip().upper()
            roles[key] = description.strip()

    lines = [
        f"# LDMS permission catalog ({len(roles)} roles). Generated from *Roles.java enums.",
        "# Run: python3 ldms-backend/ldms-user-management/scripts/generate_role_catalog.py",
    ]
    for role in sorted(roles):
        lines.append(f"{role}={roles[role]}")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {len(roles)} roles to {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
