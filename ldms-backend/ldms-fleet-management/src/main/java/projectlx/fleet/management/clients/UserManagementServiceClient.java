package projectlx.fleet.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

public interface UserManagementServiceClient {

    @GetMapping("/ldms-user-management/v1/system/user/session-profile-by-username/{username}")
    UserResponse findSessionProfileByUsername(@PathVariable("username") String username);
}
