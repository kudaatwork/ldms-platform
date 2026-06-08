package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * System request to create (or refresh) the organisation contact person user pending email verification.
 */
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
    /** When true, verification/sign-in links target the platform portal; otherwise the admin portal. */
    private Boolean viaSignup;
    /** When false, skips the contact-person verification email (credentials are emailed separately). */
    private Boolean sendVerificationEmail;
    /** When set, updates this existing user instead of creating a duplicate by email. */
    private Long contactUserId;
}
