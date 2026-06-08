package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.TwoFactorMethod;

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
    private TwoFactorMethod twoFactorMethod;
    private Long id;
    private Long userId;
}
