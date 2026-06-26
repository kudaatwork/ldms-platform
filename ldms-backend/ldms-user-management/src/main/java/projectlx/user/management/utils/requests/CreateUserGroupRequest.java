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
    /** Organisation workspace scope; platform operators may omit for classification-default groups. */
    private Long organizationId;
    /** Required for platform-admin classification-default groups; auto-set for organisation workspaces. */
    private String organizationClassification;
}
