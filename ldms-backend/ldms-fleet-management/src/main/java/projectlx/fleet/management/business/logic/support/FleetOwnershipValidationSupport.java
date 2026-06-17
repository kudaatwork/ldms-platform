package projectlx.fleet.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.fleet.management.clients.OrganizationManagementServiceClient;
import projectlx.fleet.management.utils.requests.ValidateFleetOwnershipRequest;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

/**
 * Validates fleet ownership eligibility by delegating to the organization-management
 * system endpoint. Ensures that:
 * - OWNED: allowed for SUPPLIER, TRANSPORT_COMPANY, CUSTOMER
 * - CONTRACTED: allowed for SUPPLIER and CUSTOMER only (transport company cannot
 *   register contracted assets under another party)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FleetOwnershipValidationSupport {

    private final OrganizationManagementServiceClient organizationManagementServiceClient;

    /**
     * Returns null when ownership is valid; returns a non-null error message on failure.
     */
    public String validateOwnership(Long registeringOrganizationId,
                                    String ownershipType,
                                    Long contractedTransporterOrganizationId) {
        return validateOwnership(registeringOrganizationId, ownershipType, contractedTransporterOrganizationId,
                null, null, null);
    }

    /**
     * Returns null when ownership is valid; returns a non-null error message on failure.
     */
    public String validateOwnership(Long registeringOrganizationId,
                                    String ownershipType,
                                    Long contractedTransporterOrganizationId,
                                    String contractScope,
                                    String contractStartDate,
                                    String contractEndDate) {
        try {
            ValidateFleetOwnershipRequest request = new ValidateFleetOwnershipRequest();
            request.setRegisteringOrganizationId(registeringOrganizationId);
            request.setOwnershipType(ownershipType);
            request.setContractedTransporterOrganizationId(contractedTransporterOrganizationId);
            request.setContractScope(contractScope);
            request.setContractStartDate(contractStartDate);
            request.setContractEndDate(contractEndDate);
            OrganizationResponse response = organizationManagementServiceClient.validateFleetOwnership(request);
            if (response == null || !response.isSuccess()) {
                String msg = resolveOrganizationErrorMessage(response);
                log.warn("Fleet ownership validation rejected: orgId={} ownershipType={} transporterId={} reason={}",
                        registeringOrganizationId, ownershipType, contractedTransporterOrganizationId, msg);
                return msg;
            }
            return null;
        } catch (Exception ex) {
            log.warn("Fleet ownership validation call failed for orgId={}: {}", registeringOrganizationId, ex.getMessage());
            return "Fleet ownership validation unavailable";
        }
    }

    private static String resolveOrganizationErrorMessage(OrganizationResponse response) {
        if (response == null) {
            return "Fleet ownership validation failed";
        }
        if (response.getErrorMessages() != null && !response.getErrorMessages().isEmpty()) {
            return String.join(" ", response.getErrorMessages());
        }
        if (response.getMessage() != null && !response.getMessage().isBlank()) {
            return response.getMessage().trim();
        }
        return "Fleet ownership validation failed";
    }
}
