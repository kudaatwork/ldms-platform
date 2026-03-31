package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditUserSecurityRequest {
    private String securityQuestion_1;
    private String securityAnswer_1;
    private String securityQuestion_2;
    private String securityAnswer_2;
    private String twoFactorAuthSecret;
    private Boolean isTwoFactorEnabled;
    private Long id;
    private Long userId;
}
