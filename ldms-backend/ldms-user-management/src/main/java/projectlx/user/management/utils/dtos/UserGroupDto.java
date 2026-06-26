package projectlx.user.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGroupDto {
    private Long id;
    private String name;
    private String description;
    private Long organizationId;
    private String organizationClassification;
    private boolean systemGroup;
    /** Classification default admin groups: when {@code true}, org admins cannot change inherited roles. */
    private boolean locked;
    private String systemGroupAlias;
    /** Role IDs locked as defaults on a system group (non-removable). */
    private Set<Long> defaultRoleIds;
    private List<UserRoleDto> userRoleDtoSet;
    /** Non-deleted users whose primary {@code user_group_id} points at this group. */
    private Long userMemberCount;
    /** Non-deleted catalog roles linked to this group via {@code user_group_user_role}. */
    private Long userRoleMemberCount;
}
