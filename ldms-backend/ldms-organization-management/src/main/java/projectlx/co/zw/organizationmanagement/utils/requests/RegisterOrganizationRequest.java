package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;

@Getter
@Setter
public class RegisterOrganizationRequest {

    private String name;
    private String email;
    private String phoneNumber;
    private OrganizationClassification organizationClassification;
    private Long industryId;
    private String contactPersonFirstName;
    private String contactPersonLastName;
    private String contactPersonEmail;
    private String contactPersonPhoneNumber;
}
