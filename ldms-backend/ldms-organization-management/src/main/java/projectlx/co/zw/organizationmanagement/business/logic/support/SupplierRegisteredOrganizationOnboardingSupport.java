package projectlx.co.zw.organizationmanagement.business.logic.support;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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

    public void completeOnboarding(Long organizationId) {
        if (organizationId == null || organizationId < 1) {
            return;
        }
        organizationRepository.findById(organizationId).ifPresent(this::completeOnboarding);
    }

    public void completeOnboarding(Organization org) {
        if (org == null || org.getId() == null) {
            return;
        }
        Long contactUserId = contactPersonProvisioningSupport.provisionContactPersonUser(org, true, false);
        if (contactUserId != null && contactUserId > 0) {
            linkContactUser(org.getId(), contactUserId);
            org.setContactPersonUserId(contactUserId);
        } else {
            log.error(
                    "Contact person user was not provisioned for supplier-registered organisation {} ({})",
                    org.getId(),
                    org.getContactPersonEmail());
        }

        approvedCredentialsSupport.issueAndEmailCredentialsToContactOnly(org);
        sendOrganizationVerificationEmail(org);
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

    private void sendOrganizationVerificationEmail(Organization org) {
        String organizationEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getEmail());
        if (!StringUtils.hasText(organizationEmail)) {
            log.warn("Skipping organisation verification email for orgId={}: organisation email is blank", org.getId());
            return;
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
