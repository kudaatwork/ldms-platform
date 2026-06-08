package projectlx.co.zw.organizationmanagement.business.logic.support;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.kyc.OrganizationEventPublisher;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.organizationmanagement.utils.config.OrganizationPortalLinkProperties;
import projectlx.co.zw.shared_library.business.logic.impl.TokenService;

/**
 * Post-registration onboarding for supplier-registered customers and transporters:
 * contact person receives temporary credentials; organisation inbox receives a verification link.
 */
@Component
@RequiredArgsConstructor
public class SupplierRegisteredOrganizationOnboardingSupport {

    private static final Logger log = LoggerFactory.getLogger(SupplierRegisteredOrganizationOnboardingSupport.class);
    private static final String SYSTEM_MODIFIER = "SYSTEM";

    private final OrganizationRepository organizationRepository;
    private final OrganizationServiceAuditable organizationServiceAuditable;
    private final OrganizationContactPersonProvisioningSupport contactPersonProvisioningSupport;
    private final OrganizationApprovedCredentialsSupport approvedCredentialsSupport;
    private final OrganizationKycNotifier organizationKycNotifier;
    private final OrganizationPortalLinkProperties portalLinks;
    private final OrganizationEventPublisher organizationEventPublisher;
    private final TokenService tokenService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganizationSupplierRegistered(OrganizationSupplierRegisteredEvent event) {
        if (event == null || event.organizationId() == null || event.organizationId() < 1) {
            return;
        }
        completeOnboarding(event.organizationId());
    }

    public void completeOnboarding(Long organizationId) {
        if (organizationId == null || organizationId < 1) {
            return;
        }
        organizationRepository.findById(organizationId).ifPresent(this::completeOnboarding);
    }

    public SupplierRegisteredOnboardingResult completeOnboarding(Organization org) {
        if (org == null || org.getId() == null) {
            return new SupplierRegisteredOnboardingResult(false, false, "Organisation not found.");
        }
        Organization current = organizationRepository.findById(org.getId()).orElse(org);
        log.info(
                "Starting supplier-registered onboarding for organisation {} ({})",
                current.getId(),
                current.getName());

        boolean organizationVerificationQueued = queueOrganizationVerificationEmail(current);

        Long contactUserId = resolveContactUserId(current);
        if (contactUserId == null || contactUserId <= 0) {
            log.error(
                    "Contact person user was not provisioned for supplier-registered organisation {} ({}). "
                            + "Temporary credentials email was skipped.",
                    current.getId(),
                    current.getContactPersonEmail());
            return new SupplierRegisteredOnboardingResult(
                    organizationVerificationQueued,
                    false,
                    contactProvisioningFailureMessage(current));
        }

        linkContactUser(current.getId(), contactUserId);
        current.setContactPersonUserId(contactUserId);

        boolean contactCredentialsQueued =
                approvedCredentialsSupport.issueAndEmailCredentialsToContactOnly(current);
        if (!contactCredentialsQueued) {
            return new SupplierRegisteredOnboardingResult(
                    organizationVerificationQueued,
                    false,
                    "Organisation verification was queued, but contact sign-in credentials could not be emailed. "
                            + "Check that the contact person is linked to this customer and try again.");
        }

        return new SupplierRegisteredOnboardingResult(
                organizationVerificationQueued,
                true,
                "Onboarding is complete for this customer (verification and contact access are already in place, or emails were queued).");
    }

    private Long resolveContactUserId(Organization org) {
        boolean viaSignup = Boolean.TRUE.equals(org.getCreatedViaSignup());
        return contactPersonProvisioningSupport.provisionContactPersonUser(org, viaSignup, false);
    }

    private static String contactProvisioningFailureMessage(Organization org) {
        if (org == null || !StringUtils.hasText(org.getContactPersonEmail())) {
            return "Organisation verification was queued, but no contact person email is on file.";
        }
        return "Organisation verification was queued, but the contact person could not be linked "
                + "(often because that email already belongs to another organisation). "
                + "Use a unique contact email for this customer, then retry.";
    }

    private boolean queueOrganizationVerificationEmail(Organization org) {
        if (org.isVerified()) {
            log.info("Organisation {} is already verified; skipping verification email", org.getId());
            return true;
        }
        String organizationEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getEmail());
        if (!StringUtils.hasText(organizationEmail)) {
            log.warn("Skipping organisation verification email for orgId={}: organisation email is blank", org.getId());
            return false;
        }
        String token = tokenService.generateEmailVerificationToken();
        organizationRepository.findById(org.getId()).ifPresent(reloaded -> {
            reloaded.setEmailVerificationToken(token);
            reloaded.setModifiedAt(LocalDateTime.now());
            reloaded.setModifiedBy(SYSTEM_MODIFIER);
            organizationServiceAuditable.save(reloaded);
            String verificationLink = portalLinks.buildOrganizationEmailVerificationLink(token, organizationEmail);
            organizationKycNotifier.sendOrganizationEmailVerification(reloaded, verificationLink);
        });
        return true;
    }

    private void linkContactUser(Long organizationId, Long contactUserId) {
        organizationRepository.findById(organizationId).ifPresent(reloaded -> {
            if (reloaded.getContactPersonUserId() == null || reloaded.getContactPersonUserId() <= 0) {
                reloaded.setContactPersonUserId(contactUserId);
                reloaded.setModifiedAt(LocalDateTime.now());
                reloaded.setModifiedBy(SYSTEM_MODIFIER);
                organizationServiceAuditable.save(reloaded);
            }
        });
    }

    public void markVerifiedAfterEmailConfirmation(Organization org) {
        if (org == null) {
            return;
        }
        org.setVerified(true);
        org.setEmailVerificationToken(null);
        org.setModifiedAt(LocalDateTime.now());
        org.setModifiedBy(SYSTEM_MODIFIER);
        Organization saved = organizationServiceAuditable.save(org);
        organizationEventPublisher.publishVerified(saved, LocalDateTime.now());
    }
}
