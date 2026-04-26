package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditUserRoleRequest {
    private Long id;
    private String role;
    private String description;
}
