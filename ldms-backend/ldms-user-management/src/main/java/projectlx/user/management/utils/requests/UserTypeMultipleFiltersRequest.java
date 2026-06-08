package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class UserTypeMultipleFiltersRequest extends MultipleFiltersRequest {
    private String userTypeName;
    private String description;
    /** When true, organisation workspace users see only the bootstrap {@code System Administrator} type. */
    private Boolean bootstrapDefaultsOnly;
}
