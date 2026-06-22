package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.clients.UserManagementServiceClient;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BillingApproverSupport {

    private final UserManagementServiceClient userManagementServiceClient;

    public boolean isBillingApproverForOrganization(String username, Long expectedOrganizationId) {
        return validateBillingApprover(username, expectedOrganizationId).isEmpty();
    }

    public Optional<String> validateBillingApprover(String username, Long expectedOrganizationId) {
        if (!StringUtils.hasText(username)) {
            return Optional.of("Approver username is required.");
        }
        if (expectedOrganizationId == null || expectedOrganizationId <= 0) {
            return Optional.of("Organisation context is required for payment verification.");
        }
        UserResponse response = userManagementServiceClient.findSessionProfileByUsername(username.trim());
        if (response == null || !response.isSuccess() || response.getUserDto() == null) {
            return Optional.of("Billing approver user not found.");
        }
        var approver = response.getUserDto();
        if (!Boolean.TRUE.equals(approver.getBillingApprover())) {
            return Optional.of("User is not designated as a billing approver.");
        }
        Long approverOrg = approver.getOrganizationId();
        if (approverOrg == null || !Objects.equals(approverOrg, expectedOrganizationId)) {
            return Optional.of("Billing approver must belong to the supplier organisation.");
        }
        return Optional.empty();
    }
}
