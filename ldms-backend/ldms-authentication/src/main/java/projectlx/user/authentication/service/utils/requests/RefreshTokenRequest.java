package projectlx.user.authentication.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RefreshTokenRequest {
    private String refreshToken;
    private String username;
}
