package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AddUserToUserGroupRequest {
    private Long userId;
    private Long userGroupId;
}
