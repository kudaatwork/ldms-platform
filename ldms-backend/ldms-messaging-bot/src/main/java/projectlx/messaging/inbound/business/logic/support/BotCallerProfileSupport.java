package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.messaging.inbound.clients.OrganizationManagementServiceClient;
import projectlx.messaging.inbound.clients.UserManagementServiceClient;

@Slf4j
public class BotCallerProfileSupport {

    public record CallerProfile(String displayName, String phone, Long organizationId, String organizationName) {}

    private final UserManagementServiceClient userManagementServiceClient;
    private final OrganizationManagementServiceClient organizationManagementServiceClient;

    public BotCallerProfileSupport(UserManagementServiceClient userManagementServiceClient,
                                   OrganizationManagementServiceClient organizationManagementServiceClient) {
        this.userManagementServiceClient = userManagementServiceClient;
        this.organizationManagementServiceClient = organizationManagementServiceClient;
    }

    public CallerProfile resolve(String username) {
        String displayName = username;
        String phone = "";
        Long organizationId = null;
        String organizationName = "—";
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(username);
            if (userResponse != null && userResponse.isSuccess()) {
                UserDto user = userResponse.getUserDto();
                if (user != null) {
                    displayName = buildDisplayName(user, username);
                    phone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
                    organizationId = user.getOrganizationId();
                }
            }
        } catch (Exception ex) {
            log.debug("Could not load session profile for {}: {}", username, ex.getMessage());
        }
        if (organizationId != null) {
            try {
                OrganizationResponse orgResponse = organizationManagementServiceClient.findById(organizationId);
                if (orgResponse != null && orgResponse.isSuccess()) {
                    OrganizationDto org = orgResponse.getOrganizationDto();
                    if (org != null && org.getName() != null) {
                        organizationName = org.getName();
                    }
                }
            } catch (Exception ex) {
                log.debug("Could not load organization {}: {}", organizationId, ex.getMessage());
            }
        }
        return new CallerProfile(displayName, phone, organizationId, organizationName);
    }

    private static String buildDisplayName(UserDto user, String fallback) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isBlank() ? fallback : combined;
    }
}
