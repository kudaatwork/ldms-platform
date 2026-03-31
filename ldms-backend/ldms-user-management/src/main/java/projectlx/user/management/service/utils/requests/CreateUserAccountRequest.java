package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateUserAccountRequest {
    private String phoneNumber;
    private String accountNumber;
    private Boolean isAccountLocked;
    private Long userId;
}
