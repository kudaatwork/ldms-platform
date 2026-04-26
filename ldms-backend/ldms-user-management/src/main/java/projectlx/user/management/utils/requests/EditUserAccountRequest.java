package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditUserAccountRequest {
    private Long id;
    private String phoneNumber;
    private String accountNumber;
    private Boolean isAccountLocked;
    private Long userId;
}
