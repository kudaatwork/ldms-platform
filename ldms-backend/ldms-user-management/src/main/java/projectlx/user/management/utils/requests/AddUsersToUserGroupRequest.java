package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class AddUsersToUserGroupRequest {
    private Long userGroupId;
    private List<Long> userIds;
}
