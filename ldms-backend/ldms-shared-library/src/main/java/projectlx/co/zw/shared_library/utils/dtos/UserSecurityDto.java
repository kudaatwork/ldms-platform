package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import projectlx.co.zw.shared_library.utils.enums.TwoFactorMethod;
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
    @JsonProperty("isTwoFactorEnabled")
    private Boolean isTwoFactorEnabled;
    private TwoFactorMethod twoFactorMethod;
}
