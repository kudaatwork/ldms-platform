package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
public class ForgotPasswordRequest {
    
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail; // Can be either username or email
}