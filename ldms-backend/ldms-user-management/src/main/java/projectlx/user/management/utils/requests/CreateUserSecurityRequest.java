package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateUserSecurityRequest {
    private String securityQuestion_1;
    private String securityAnswer_1;
    private String securityQuestion_2;
    private String securityAnswer_2;
    private String twoFactorAuthSecret;
    private Boolean isTwoFactorEnabled;
    private Long userId;
}
