package projectlx.user.authentication.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AuthRequest {

    /**
     * Sign-in identifier: LDMS username or registered email address (same field for both).
     */
    private String username;
    private String password;
}
