package projectlx.co.zw.organizationmanagement.clients;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProvisionOrganizationContactPersonRequest {

    private Long organizationId;
    private String organizationName;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private String nationalIdNumber;
    private String passportNumber;
    /** ISO date {@code yyyy-MM-dd}. */
    private String dateOfBirth;
    private Long nationalIdUploadId;
    private Long passportUploadId;
    private Boolean viaSignup;
    private Boolean sendVerificationEmail;
}
