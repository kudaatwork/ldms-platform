package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** System-only: issued by ldms-authentication after successful password validation. */
@Data
public class GenerateLoginOtpRequest {

    @NotBlank(message = "Username or phone is required.")
    private String usernameOrPhone;
}
