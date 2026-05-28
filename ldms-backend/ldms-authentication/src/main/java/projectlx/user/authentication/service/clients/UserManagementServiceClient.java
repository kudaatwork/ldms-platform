package projectlx.user.authentication.service.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

/**
 * Feign client for user-management system API. Base URL is set in
 * {@link projectlx.user.authentication.service.utils.config.UserManagementServiceFeignConfiguration}.
 */
public interface UserManagementServiceClient {

	@GetMapping("/ldms-user-management/v1/system/user/find-by-username/{username}")
	UserResponse findByUsername(@PathVariable("username") String username);

	@GetMapping("/ldms-user-management/v1/system/user/find-by-phone-number-or-email/{phoneNumberOrEmail}")
	UserResponse findByPhoneNumberOrEmail(@PathVariable("phoneNumberOrEmail") String phoneNumberOrEmail);
}
