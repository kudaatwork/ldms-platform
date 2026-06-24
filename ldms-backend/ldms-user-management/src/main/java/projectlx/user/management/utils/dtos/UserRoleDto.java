package projectlx.user.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.EntityStatus;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoleDto {
    private Long id;
    private String role;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;
    /** Module key for admin UI grouping (derived from role code, not stored in DB). */
    private String moduleKey;
    /** Human-readable module label for admin UI grouping. */
    private String moduleLabel;
    /** Organization classifications this role applies to. Empty/null = platform-only. */
    private Set<String> organizationClassifications;
    /** True when the role can belong to an organisation classification (editable in the admin drill-down). */
    private Boolean organizationPortalRole;
}
