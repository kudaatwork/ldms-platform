package projectlx.user.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSecurityDto {
    private Long id;
    private String securityQuestion_1;
    private String securityAnswer_1;
    private String securityQuestion_2;
    private String securityAnswer_2;
    private String twoFactorAuthSecret;
    private Boolean isTwoFactorEnabled;
}
