# Platform portal — supplier-registered organisation onboarding

When a **supplier** registers a **customer** or **transporter** on the **platform portal**, this flow applies (not self-signup KYC).

## Trigger

- Supplier registers customer or transporter in the platform portal.
- Organisation is saved with KYC approved but email not yet verified; credentials are issued after onboarding completes.

## Two parallel email tracks

| Recipient | What they receive |
|-----------|-------------------|
| **Contact person** | Temporary username, temporary password, and a **Sign in** button |
| **Organisation email** | Verification link for the organisation inbox |

## Contact person sign-in path

1. First login with temporary credentials.
2. JWT includes `mustChangeCredentials: true`.
3. Platform routes to **Setup credentials** (`/auth/setup-credentials`).
4. User sets permanent username and password.
5. Re-login → normal dashboard access.

## Organisation verification path

1. Organisation inbox clicks the verification link (`/auth/verify-organization-email`).
2. Organisation is marked **verified** and an `org.verified` event is published.

## Important rules

- Contact credentials go to the **contact person only** (not the org inbox).
- Org verification email goes to the **organisation email only**.
- Supplier-registered orgs skip platform KYC review but still require **org email verification**.
- Same onboarding for **customer** and **transporter** registrations.
- Contact person email must not already be linked to another organisation's user.

## Where users get help

- **Help & Support → Assistant** for LDMS workflow questions.
- **Help & Support → New ticket** for account-specific or unresolved issues.
