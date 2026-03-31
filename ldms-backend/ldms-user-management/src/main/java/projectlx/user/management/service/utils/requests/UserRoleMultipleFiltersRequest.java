package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class UserRoleMultipleFiltersRequest extends MultipleFiltersRequest {
    private String role;
    private String description;
}
