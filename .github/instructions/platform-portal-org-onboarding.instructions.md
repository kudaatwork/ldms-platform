---
description: "Supplier-initiated customer/transporter onboarding on the platform portal (credentials + org email verification)"
applyTo: "ldms-backend/ldms-organization-management/**/*.java"
---

# Platform portal — supplier-registered organisation onboarding

When a **supplier** registers a **customer** or **transporter** on the **platform portal**, use this flow. Do not conflate it with self-signup KYC (credentials after full KYC approval).

## Trigger

- `POST` register customer / transporter (`OrganizationFrontendResource`)
- `OrganizationServiceImpl.registerCustomer` / `registerTransporter`
- Org saved: `kycStatus = APPROVED`, `verified = false`, `createdViaSignup = false`
- Post-commit: `SupplierRegisteredOrganizationOnboardingSupport.completeOnboarding(orgId)`

## Two parallel email tracks

| Recipient | Content | Backend |
|-----------|---------|---------|
| **Contact person** | Temporary username, temporary password, **Sign in** button (`signInLink`) | `OrganizationApprovedCredentialsSupport.issueAndEmailCredentialsToContactOnly` → `UserManagementServiceClient.issueOrganizationContactCredentials` → `OrganizationKycNotifier.sendContactCredentials` (template `ORG_KYC_APPROVED_CREDENTIALS`) |
| **Organisation email** | Verification link for org inbox | `sendOrganizationVerificationEmail` → `OrganizationKycNotifier.sendOrganizationEmailVerification` (template `ORG_EMAIL_VERIFICATION`) |

## Contact person sign-in path

1. First login with temporary credentials.
2. Auth JWT includes `mustChangeCredentials: true` (`AuthenticationServiceImpl`, `OrganizationContactCredentialsIssuer.issueTemporaryCredentials`).
3. Platform portal routes to `/auth/setup-credentials` (`SetupCredentialsGuard`, `AuthGuard`).
4. User sets **permanent username + password** (`UserFrontendResource.completeCredentialsSetup` → `OrganizationContactCredentialsIssuer.completeCredentialsSetup`).
5. Re-login → normal dashboard access.

## Organisation verification path

1. Org inbox clicks link → platform `/auth/verify-organization-email?token=&email=`.
2. `OrganizationSystemResource.verifyOrganizationEmail` → `markVerifiedAfterEmailConfirmation` → `verified = true`, publishes `org.verified`.

## Do not break

- Contact credentials email goes to **contact person only** on supplier registration (not org inbox).
- Org verification email goes to **organisation email only**.
- Supplier-registered orgs skip platform KYC review but still require **org email verification**.
- Same onboarding for **customer** and **transporter** (both call `completeOnboarding` after commit).
- **`ORG_EMAIL_VERIFICATION`** notification template must exist (Flyway V17 on `ldms_notifications`). Without it, org verification emails fail in the consumer.
- Contact person email must **not** already be linked to another organisation's user — registration should reject with `org.contactEmailLinked`.

## Key files

- Backend: `SupplierRegisteredOrganizationOnboardingSupport`, `OrganizationApprovedCredentialsSupport`, `OrganizationContactCredentialsIssuer`, `OrganizationKycNotifier`, `OrganizationSupplierRegisteredEvent`
- Retry (system): `POST .../system/organization/{id}/complete-supplier-onboarding`
- Frontend: `auth.service.ts` (post-login route), `setup-credentials/*`, `verify-organization-email/*`, `credentials-setup.service.ts`
