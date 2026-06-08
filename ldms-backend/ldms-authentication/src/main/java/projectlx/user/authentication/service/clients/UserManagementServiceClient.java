package projectlx.user.authentication.service.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	/**
	 * Triggers LOGIN_2FA OTP generation and SMS delivery for the given user.
	 * Called immediately after password success when 2FA is enabled.
	 */
	@PostMapping("/ldms-user-management/v1/system/user/otp/generate-login-otp")
	UserResponse generateLoginOtp(@RequestParam("usernameOrPhone") String usernameOrPhone);

	/**
	 * Verifies a LOGIN_2FA OTP submitted by the user.  Returns success=true when the OTP
	 * matches and has not expired.
	 */
	@PostMapping("/ldms-user-management/v1/system/user/otp/verify-login-otp")
	UserResponse verifyLoginOtp(@RequestParam("usernameOrPhone") String usernameOrPhone,
	                            @RequestParam("otp") String otp);
}
