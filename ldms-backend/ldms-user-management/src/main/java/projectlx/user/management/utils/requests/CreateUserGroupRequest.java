package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateUserGroupRequest {
    private String name;
    private String description;
    /** Organisation workspace scope; platform operators may omit for global groups. */
    private Long organizationId;
}
