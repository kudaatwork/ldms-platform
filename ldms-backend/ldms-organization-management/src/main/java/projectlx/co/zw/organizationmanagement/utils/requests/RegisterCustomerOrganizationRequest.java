package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCustomerOrganizationRequest {

    private String name;
    private String email;
    private String phoneNumber;
}
