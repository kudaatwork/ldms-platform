package projectlx.co.zw.organizationmanagement.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.clients.IssueOrganizationContactCredentialsRequest;
import projectlx.co.zw.organizationmanagement.clients.UserManagementServiceClient;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

/**
 * After final KYC approval, issues temporary portal credentials and emails them to the organisation
 * and contact person addresses.
 */
@Component
@RequiredArgsConstructor
public class OrganizationApprovedCredentialsSupport {

    private static final Logger log = LoggerFactory.getLogger(OrganizationApprovedCredentialsSupport.class);

    private final OrganizationContactPersonProvisioningSupport contactPersonProvisioningSupport;
    private final UserManagementServiceClient userManagementServiceClient;
    private final OrganizationKycNotifier organizationKycNotifier;

    public void issueAndEmailCredentials(Organization org) {
        if (org == null || org.getId() == null) {
            return;
        }
        boolean viaSignup = Boolean.TRUE.equals(org.getCreatedViaSignup());
        Long contactUserId = org.getContactPersonUserId();
        if (contactUserId == null || contactUserId <= 0) {
            contactUserId = contactPersonProvisioningSupport.provisionContactPersonUser(org, viaSignup);
        }
        if (contactUserId == null || contactUserId <= 0) {
            log.warn(
                    "Skipping approved-credentials email for organisation {}: contact person user is not linked",
                    org.getId());
            return;
        }

        IssueOrganizationContactCredentialsRequest request = new IssueOrganizationContactCredentialsRequest();
        request.setOrganizationId(org.getId());
        request.setContactUserId(contactUserId);
        UserResponse credentialsResponse;
        try {
            credentialsResponse = userManagementServiceClient.issueOrganizationContactCredentials(request);
        } catch (Exception ex) {
            log.error(
                    "Failed to issue temporary credentials for organisation {} contact user {}: {}",
                    org.getId(),
                    contactUserId,
                    ex.getMessage(),
                    ex);
            return;
        }
        if (credentialsResponse == null
                || !credentialsResponse.isSuccess()
                || !StringUtils.hasText(credentialsResponse.getTemporaryUsername())
                || !StringUtils.hasText(credentialsResponse.getTemporaryPassword())) {
            log.warn(
                    "Temporary credentials were not returned for organisation {}: {}",
                    org.getId(),
                    credentialsResponse != null ? credentialsResponse.getMessage() : "null response");
            return;
        }

        organizationKycNotifier.sendApprovedCredentials(
                org,
                credentialsResponse.getTemporaryUsername().trim(),
                credentialsResponse.getTemporaryPassword().trim());
    }
}
