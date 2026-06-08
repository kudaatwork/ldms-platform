package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TwoFactorOtpRequest {

    @NotBlank
    private String otp;
}
