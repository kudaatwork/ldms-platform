package projectlx.co.zw.organizationmanagement.business.logic.support;

/**
 * Outcome of supplier-registered onboarding email delivery.
 */
public record SupplierRegisteredOnboardingResult(
        boolean organizationVerificationQueued,
        boolean contactCredentialsQueued,
        String detailMessage) {

    public boolean isFullySuccessful() {
        return organizationVerificationQueued && contactCredentialsQueued;
    }
}
