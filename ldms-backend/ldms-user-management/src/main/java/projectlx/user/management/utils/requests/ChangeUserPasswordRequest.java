package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChangeUserPasswordRequest {
    private Long id;
    private String password;
    private String oldPassword;
    private Long userId;
}
