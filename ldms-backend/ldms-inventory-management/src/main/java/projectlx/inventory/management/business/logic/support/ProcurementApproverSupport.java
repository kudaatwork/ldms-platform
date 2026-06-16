package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.inventory.management.clients.UserManagementServiceClient;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProcurementApproverSupport {

    private final UserManagementServiceClient userManagementServiceClient;

    public Optional<String> validateApproverForOrganization(Long userId, Long expectedOrganizationId, Locale locale) {
        if (userId == null || userId <= 0) {
            return Optional.of("Approver user id is required.");
        }
        if (expectedOrganizationId == null || expectedOrganizationId <= 0) {
            return Optional.of("Organisation context is required for procurement approval.");
        }
        UserResponse response = userManagementServiceClient.findById(userId, locale);
        if (response == null || !response.isSuccess() || response.getUserDto() == null) {
            return Optional.of("Approver user not found.");
        }
        UserDto approver = response.getUserDto();
        if (!Boolean.TRUE.equals(approver.getProcurementApprover())) {
            return Optional.of("User is not designated as a procurement approver.");
        }
        Long approverOrg = approver.getOrganizationId();
        if (approverOrg == null || !Objects.equals(approverOrg, expectedOrganizationId)) {
            return Optional.of("Procurement approver must belong to the same organisation as the document.");
        }
        return Optional.empty();
    }
}
