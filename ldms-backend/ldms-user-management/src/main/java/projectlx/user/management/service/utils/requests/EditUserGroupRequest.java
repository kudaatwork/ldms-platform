package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditUserGroupRequest {
    private Long id;
    private String name;
    private String description;
}
