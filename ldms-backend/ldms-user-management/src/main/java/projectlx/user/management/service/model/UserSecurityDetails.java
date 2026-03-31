package projectlx.user.management.service.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserSecurityDetails {
    private String securityQuestion_1; // First security question
    private String securityAnswer_1; // Answer to the first security question
    private String securityQuestion_2; // Second security question
    private String securityAnswer_2; // Answer to the second security question
    private String twoFactorAuthSecret; // Secret for two-factor authentication
    private Boolean isTwoFactorEnabled; // Indicates if 2FA is enabled
}
