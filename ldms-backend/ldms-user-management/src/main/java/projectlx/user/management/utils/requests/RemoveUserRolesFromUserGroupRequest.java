package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Getter
@Setter
@ToString
public class RemoveUserRolesFromUserGroupRequest {
    private List<Long> userRoleIds;
    private Long userGroupId;
}
