package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoleDto {
    private Long id;
    private String role;
    private String description;
    /** Organization classifications this role applies to. Empty/null = platform-only. */
    private Set<String> organizationClassifications;
}
