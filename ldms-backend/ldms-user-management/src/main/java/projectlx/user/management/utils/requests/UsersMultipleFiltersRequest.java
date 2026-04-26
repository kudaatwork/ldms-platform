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
}
