package projectlx.user.authentication.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

@FeignClient(name = "user-management-service", url = "${clients.baseUrl.userManagementService}")
public interface UserManagementServiceClient {
    @GetMapping("/find-by-username/{username}")
    UserResponse findByUsername(@PathVariable("username") String username);
}
