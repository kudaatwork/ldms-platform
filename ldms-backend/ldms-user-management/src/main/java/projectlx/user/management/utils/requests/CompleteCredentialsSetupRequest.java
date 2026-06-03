package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteCredentialsSetupRequest {

    @NotBlank
    private String newUsername;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
