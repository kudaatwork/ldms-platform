package projectlx.user.authentication.service.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.authentication.service.utils.dtos.AuthDto;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse extends CommonResponse {
    private AuthDto authDto;
    private List<AuthDto> authDtoList;
    private Page<AuthDto> authDtoPage;
    private String accessToken;
    private String refreshToken;
    /** When true, the client must redirect to credential setup before using the portal. */
    private Boolean mustChangeCredentials;
    /**
     * When true, the client must present the {@link #mfaChallengeToken} together with the
     * SMS OTP at {@code POST /auth/verify-two-factor} to complete the login.
     */
    private Boolean requiresTwoFactor;
    /**
     * Short-lived opaque token representing the pending 2FA challenge.
     * Present only when {@link #requiresTwoFactor} is true.
     */
    private String mfaChallengeToken;
    /**
     * {@code SMS} or {@code AUTHENTICATOR_APP} — tells the client how to complete the 2FA step.
     */
    private String twoFactorMethod;
}
