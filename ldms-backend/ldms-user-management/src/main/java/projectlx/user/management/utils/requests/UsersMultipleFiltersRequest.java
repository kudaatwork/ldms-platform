package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

import java.util.List;

@Getter
@Setter
@ToString
public class UsersMultipleFiltersRequest extends MultipleFiltersRequest {
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private List<String> gender;
    private String phoneNumber;
    private String nationalIdNumber;
    private String passportNumber;
    private String entityStatus;
    /** When set, only users whose primary user group matches this id. */
    private Long userGroupId;
    /** When set, only users linked to this organisation id. */
    private Long organizationId;
    /** When set, only users linked to this branch id. */
    private Long branchId;
}
