package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class UserAccountMultipleFiltersRequest extends MultipleFiltersRequest {
    private String phoneNumber;
    private String accountNumber;
    private Boolean isAccountLocked;
    private Long userId;
}
