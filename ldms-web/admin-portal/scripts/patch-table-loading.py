#!/usr/bin/env python3
"""Patch list pages to use lx-table-loading component."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TAG = "motion".replace("motion", "div")

PATCHES: list[tuple[str, str, str, str, str]] = [
    (
        "src/app/features/users/pages/users-list/users-list.component.html",
        "fetching && userTable.data.length === 0",
        "Loading users…",
        "fetching && userTable.data.length > 0",
        "Refreshing users…",
    ),
    (
        "src/app/features/users/pages/users-roles/users-roles.component.html",
        "fetching && dataSource.length === 0",
        "Loading roles…",
        "fetching && dataSource.length > 0",
        "Refreshing roles…",
    ),
    (
        "src/app/features/users/pages/users-groups/users-groups.component.html",
        "fetching && groups.length === 0",
        "Loading user groups…",
        "fetching && groups.length > 0",
        "Refreshing user groups…",
    ),
    (
        "src/app/features/users/pages/user-types/user-types.component.html",
        "fetching && rows.length === 0",
        "Loading user types…",
        "fetching && rows.length > 0",
        "Refreshing user types…",
    ),
    (
        "src/app/features/activity/pages/activity-page/activity-page.component.html",
        "requestLoading && requestRawRows.length === 0",
        "Loading requests log…",
        "requestLoading && requestRawRows.length > 0",
        "Refreshing requests log…",
    ),
    (
        "src/app/features/activity/pages/churnout-history-page/churnout-history-page.component.html",
        "loading && dataSource.data.length === 0",
        "Loading churnout history…",
        "loading && dataSource.data.length > 0",
        "Refreshing churnout history…",
    ),
    (
        "src/app/features/locations/components/location-table-page/location-table-page.component.html",
        "fetching && rawRows.length === 0",
        "Loading records…",
        "fetching && rawRows.length > 0",
        "Refreshing records…",
    ),
    (
        "src/app/features/users/pages/user-profile-shell/user-profile-shell.component.html",
        "loading",
        "Loading user workspace…",
        "loading",
        "Loading user workspace…",
    ),
]

LOADING_RE = re.compile(
    r'<div class="lx-table-loading" \*ngIf="[^"]+">\s*'
    r'<mat-progress-bar[^>]*></mat-progress-bar>\s*'
    r'<p class="lx-table-loading__text">[^<]*</p>\s*'
    r"</div>\s*",
    re.MULTILINE,
)


def patch_file(rel: str, init_busy: str, init_msg: str, ref_busy: str, ref_msg: str) -> None:
    path = ROOT / rel
    text = path.read_text()
    replacement = (
        f'<lx-table-loading [busy]="{init_busy}" message="{init_msg}"></lx-table-loading>\n'
    )
    new_text, n = LOADING_RE.subn(replacement, text, count=1)
    if n == 0:
        print(f"skip (no block): {rel}")
        return
    if "lx-table-wrap--host" not in new_text and "lx-table-wrap" in new_text:
        new_text = new_text.replace(
            '<div class="lx-table-wrap">',
            f'<{TAG} class="lx-table-wrap lx-table-wrap--host">'
            f'<lx-table-loading [busy]="{ref_busy}" [hasData]="true" message="{ref_msg}"></lx-table-loading>',
            1,
        )
        new_text = new_text.replace(
            '<motion class="lx-table-wrap lx-users-table-wrap">',
            f'<{TAG} class="lx-table-wrap lx-users-table-wrap lx-table-wrap--host">'
            f'<lx-table-loading [busy]="{ref_busy}" [hasData]="true" message="{ref_msg}"></lx-table-loading>',
            1,
        )
    path.write_text(new_text)
    print(f"patched: {rel}")


def main() -> None:
    for args in PATCHES:
        patch_file(*args)


if __name__ == "__main__":
    main()
