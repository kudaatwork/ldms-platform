#!/usr/bin/env python3
"""Generates Flyway migration SQL for Project LX branded organisation email templates."""

from __future__ import annotations

import sys

HEADER = """<tr>
  <td style="background:linear-gradient(135deg,#0f1c45 0%,#1b2a5e 50%,#2c3d85 100%);padding:32px 48px 30px">
    <table cellpadding="0" cellspacing="0"><tr>
      <td style="background:#3b82f6;border-radius:10px;padding:10px 16px;font-size:20px;font-weight:800;color:#ffffff;font-family:Arial,sans-serif;letter-spacing:-0.5px">LX</td>
      <td style="padding-left:16px;vertical-align:middle">
        <div style="font-size:19px;font-weight:700;color:#ffffff;font-family:Arial,sans-serif">Project LX</div>
        <div style="font-size:11px;color:rgba(255,255,255,0.48);font-family:Arial,sans-serif;margin-top:3px;letter-spacing:0.2px">Logistics &amp; Distribution Platform</div>
      </td>
    </tr></table>
  </td>
</tr>"""

ACCENT = """<tr><td height="4" style="background:linear-gradient(90deg,#3b82f6 0%,#818cf8 48%,#f97316 100%);font-size:0;line-height:0">&nbsp;</td></tr>"""

FOOTER = """<tr><td style="background:#f8fafc;border-top:1px solid #e8eef4;padding:24px 48px">
  <p style="margin:0;font-size:11px;color:#94a3b8;text-align:center;line-height:1.75;font-family:Arial,sans-serif">
    &copy; 2025 Project LX &middot; Logistics &amp; Distribution Management System<br />
    Sent to <strong>{{Email}}</strong> because it is registered on Project LX.
  </p>
</td></tr>"""


def info_rows(rows: list[tuple[str, str]]) -> str:
    parts = ['<table width="100%" cellpadding="0" cellspacing="0">']
    for i, (label, value) in enumerate(rows):
        border = "" if i == len(rows) - 1 else "border-bottom:1px solid #e2e8f0;"
        parts.append(
            f'<tr><td style="padding:13px 22px;font-size:13px;color:#64748b;font-family:Arial,sans-serif;{border}">{label}</td>'
            f'<td align="right" style="padding:13px 22px;font-size:13px;font-weight:600;color:#0f1c45;font-family:Arial,sans-serif;{border}">{value}</td></tr>'
        )
    parts.append("</table>")
    return "".join(parts)


def warning(title: str, body: str) -> str:
    return f"""<table width="100%" cellpadding="0" cellspacing="0" style="background:#fef2f2;border:1.5px solid #fecaca;border-radius:12px;margin-bottom:26px"><tr><td style="padding:18px 22px"><table cellpadding="0" cellspacing="0"><tr><td style="width:34px;vertical-align:top;padding-top:1px"><div style="width:26px;height:26px;background:#ef4444;border-radius:50%;text-align:center;line-height:26px;font-size:13px;color:#fff;font-weight:700">!</div></td><td style="padding-left:12px"><p style="margin:0 0 4px;font-size:13px;font-weight:700;color:#991b1b;font-family:Arial,sans-serif">{title}</p><p style="margin:0;font-size:13px;color:#b91c1c;line-height:1.6;font-family:Arial,sans-serif">{body}</p></td></tr></table></td></tr></table>"""


def build(
    badge_label: str,
    badge_bg: str,
    badge_border: str,
    badge_color: str,
    greeting: str,
    body: str,
    rows: list[tuple[str, str]],
    cta_label: str,
    cta_href: str,
    fallback_token: str,
    fallback_hint: str = "Trouble with the button? Paste this link into your browser:",
    warn: tuple[str, str] | None = None,
) -> str:
    warn_html = warning(warn[0], warn[1]) if warn else ""
    return (
        '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width,initial-scale=1.0" /></head>'
        '<body style="margin:0;padding:0;background:#edf2f7;font-family:Arial,Helvetica,sans-serif">'
        '<table width="100%" cellpadding="0" cellspacing="0" style="background:#edf2f7"><tr><td align="center" style="padding:40px 16px">'
        '<table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:18px;overflow:hidden">'
        + HEADER
        + ACCENT
        + '<tr><td style="padding:38px 48px 44px">'
        + f'<table cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td style="background:{badge_bg};border:1.5px solid {badge_border};border-radius:20px;padding:6px 16px">'
        + f'<span style="font-size:11px;font-weight:700;color:{badge_color};font-family:Arial,sans-serif;letter-spacing:0.4px">{badge_label}</span></td></tr></table>'
        + f'<p style="margin:0 0 14px;font-size:27px;font-weight:700;color:#0f1c45;font-family:Arial,sans-serif;line-height:1.25">Hi {greeting},</p>'
        + body
        + '<table width="100%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border:1.5px solid #e2e8f0;border-radius:12px;margin-bottom:30px"><tr><td>'
        + info_rows(rows)
        + '</td></tr></table>'
        + '<table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:26px"><tr><td align="center"><table cellpadding="0" cellspacing="0"><tr>'
        + '<td style="background:linear-gradient(135deg,#1b2a5e 0%,#2c3d85 100%);border-radius:50px">'
        + f'<a href="{cta_href}" style="display:inline-block;padding:16px 52px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;font-family:Arial,sans-serif;letter-spacing:0.3px">{cta_label}</a>'
        + '</td></tr></table></td></tr></table>'
        + warn_html
        + f'<p style="margin:0;font-size:13px;color:#94a3b8;line-height:1.65;font-family:Arial,sans-serif;text-align:center">{fallback_hint}<br />'
        + f'<span style="color:#3b82f6;word-break:break-all;font-size:12px">{{{{{fallback_token}}}}}</span></p>'
        + '</td></tr>'
        + FOOTER
        + '</table></td></tr></table></body></html>'
    )


TEMPLATES = {
    "ORGANIZATION_SIGNUP_RECEIVED": build(
        "&#128233; REGISTRATION RECEIVED",
        "#eff6ff",
        "#bfdbfe",
        "#1e40af",
        "{{contactName}}",
        '<p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">Thank you for registering <strong style="color:#1b2a5e">{{organizationName}}</strong> on Project LX LDMS. Complete your application and submit KYC when you are ready. We will guide you through the next steps on the platform.</p>',
        [("Organisation", "{{organizationName}}"), ("Email", "{{Email}}")],
        "Continue application &rarr;",
        "{{nextStepsLink}}",
        "signInLink",
        fallback_hint="Sign in later:",
    ),
    "ORGANIZATION_REGISTERED_BY_ADMIN": build(
        "&#9989; ORGANISATION REGISTERED",
        "#ecfdf5",
        "#a7f3d0",
        "#065f46",
        "{{contactName}}",
        '<p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">Organisation <strong style="color:#1b2a5e">{{organizationName}}</strong> has been registered on Project LX LDMS by an administrator. You can sign in when your portal access is ready.</p>',
        [("Organisation", "{{organizationName}}"), ("Email", "{{Email}}")],
        "Sign in to Project LX &rarr;",
        "{{signInLink}}",
        "signInLink",
    ),
    "ORG_CONTACT_PERSON_VERIFICATION": build(
        "&#128231; VERIFY YOUR EMAIL",
        "#eff6ff",
        "#bfdbfe",
        "#1e40af",
        "{{firstName}}",
        '<p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">You have been listed as the contact person for <strong style="color:#1b2a5e">{{organizationName}}</strong> on Project LX LDMS. Verify your email to activate your user account and access your organisation portal.</p>',
        [("Organisation", "{{organizationName}}"), ("Email", "{{Email}}")],
        "Verify My Email &rarr;",
        "{{verificationLink}}",
        "verificationLink",
        warn=(
            "Did not expect this?",
            "If you were not named as a contact person for this organisation, you can ignore this message. No account changes will be made unless you verify.",
        ),
    ),
    "ORG_KYC_STAGE1_APPROVED": build(
        "&#10003; KYC STAGE 1 APPROVED",
        "#ecfdf5",
        "#a7f3d0",
        "#065f46",
        "{{contactName}}",
        '<p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;">Good news: the KYC review for <strong style="color:#1b2a5e">{{organizationName}}</strong> has completed <strong style="color:#1b2a5e">Stage 1</strong> successfully. Your application is now in <strong style="color:#047857">Stage 2 review</strong>. We will notify you when the next decision is made.</p>',
        [("Organisation", "{{organizationName}}"), ("KYC status", "Stage 2 review")],
        "View application &rarr;",
        "{{nextStepsLink}}",
        "nextStepsLink",
    ),
    "ORG_KYC_STAGE2_APPROVED": build(
        "&#127942; ORGANISATION VERIFIED",
        "#ecfdf5",
        "#a7f3d0",
        "#065f46",
        "{{contactName}}",
        '<p style="margin:0 0 26px;font-size:15px;color:#4a5568;line-height:1.8;font-family:Arial,sans-serif;"><strong style="color:#1b2a5e">{{organizationName}}</strong> has completed KYC and is now <strong style="color:#047857">verified</strong> on Project LX LDMS. You can sign in and use the platform.</p>',
        [("Organisation", "{{organizationName}}"), ("Status", "Verified")],
        "Sign in to Project LX &rarr;",
        "{{signInLink}}",
        "signInLink",
    ),
}


def sql_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def main() -> None:
    output = sys.argv[1] if len(sys.argv) > 1 else (
        "src/main/resources/db/migration/V10__organization_branded_email_templates_all.sql"
    )
    lines = [
        "-- Project LX branded HTML for all organisation-related notification templates.",
        "",
    ]
    for key, html in TEMPLATES.items():
        lines.append("UPDATE notification_template")
        lines.append(f"SET email_body_html = '{sql_escape(html)}',")
        lines.append("    updated_at = NOW(6)")
        lines.append(f"WHERE template_key = '{key}';")
        lines.append("")
    with open(output, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"Wrote {output} ({len(TEMPLATES)} templates)")


if __name__ == "__main__":
    main()
