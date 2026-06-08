package projectlx.co.zw.fleetmanagement.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.fleetmanagement.clients.UserManagementServiceClient;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallerOrganizationResolver {

    private final UserManagementServiceClient userManagementServiceClient;

    public Long resolveCallerOrganizationId(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        String principal = username.trim();
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(principal);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getOrganizationId() != null
                    && userResponse.getUserDto().getOrganizationId() > 0) {
                return userResponse.getUserDto().getOrganizationId();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve organisation id for user {} via user-management: {}", principal, ex.getMessage());
        }
        return null;
    }

    public Long requireCallerOrganizationId(String username) {
        Long organizationId = resolveCallerOrganizationId(username);
        if (organizationId == null || organizationId < 1) {
            return null;
        }
        return organizationId;
    }
}
