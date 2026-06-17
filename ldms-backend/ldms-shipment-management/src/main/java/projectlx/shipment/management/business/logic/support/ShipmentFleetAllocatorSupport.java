package projectlx.shipment.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.shipment.management.clients.UserManagementServiceClient;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ShipmentFleetAllocatorSupport {

    private final UserManagementServiceClient userManagementServiceClient;

    public Optional<String> validateCallerCanAllocate(String username, Locale locale) {
        if (!StringUtils.hasText(username)) {
            return Optional.of("Signed-in user is required to allocate fleet.");
        }
        UserResponse response = userManagementServiceClient.findSessionProfileByUsername(username.trim());
        if (response == null || !response.isSuccess() || response.getUserDto() == null) {
            return Optional.of("User profile not found.");
        }
        UserDto caller = response.getUserDto();
        if (Boolean.TRUE.equals(caller.getShipmentFleetAllocator())
                || Boolean.TRUE.equals(caller.getOrganizationWorkspaceAdministrator())) {
            return Optional.empty();
        }
        return Optional.of("User is not designated as a shipment fleet allocator.");
    }
}
