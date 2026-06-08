package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** System-only: verifies a LOGIN_2FA OTP on behalf of ldms-authentication. */
@Data
public class VerifyLoginOtpRequest {

    @NotBlank(message = "Username or phone is required.")
    private String usernameOrPhone;

    @NotBlank(message = "OTP is required.")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits.")
    private String otp;
}
