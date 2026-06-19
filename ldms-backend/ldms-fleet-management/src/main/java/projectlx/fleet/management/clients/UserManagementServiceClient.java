package projectlx.fleet.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.fleet.management.clients.dto.ProvisionDriverPlatformUserRequest;

public interface UserManagementServiceClient {

    @GetMapping("/ldms-user-management/v1/system/user/session-profile-by-username/{username}")
    UserResponse findSessionProfileByUsername(@PathVariable("username") String username);

    @PostMapping("/ldms-user-management/v1/system/user/provision-driver-platform-access")
    UserResponse provisionDriverPlatformAccess(@RequestBody ProvisionDriverPlatformUserRequest request);
}
