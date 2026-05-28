package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;
import projectlx.co.zw.organizationmanagement.model.OrganizationType;
import projectlx.co.zw.shared_library.utils.enums.Gender;

@Getter
@Setter
public class RegisterOrganizationRequest {

    private String name;
    private String email;
    private String phoneNumber;
    private OrganizationClassification organizationClassification;
    /** Legal form / entity type (private company, government, NGO, etc.). */
    private OrganizationType organizationType;
    private Long industryId;
    private String contactPersonFirstName;
    private String contactPersonLastName;
    private String contactPersonEmail;
    private String contactPersonPhoneNumber;
    private Gender contactPersonGender;
    /** ISO date {@code yyyy-MM-dd}; contact person must be at least 18. */
    private String contactPersonDateOfBirth;
    /** Optional; stored on the national ID file-upload row. */
    private String contactPersonNationalIdExpiryDate;
    /** Optional; stored on the passport file-upload row. */
    private String contactPersonPassportExpiryDate;
    private String contactPersonNationalIdNumber;
    private MultipartFile contactPersonNationalIdUpload;
    private Long contactPersonNationalIdUploadId;
    private String contactPersonPassportNumber;
    private MultipartFile contactPersonPassportUpload;
    private Long contactPersonPassportUploadId;
    private String registrationNumber;
    private String taxNumber;
    /** When false, organisation was registered by an administrator (not public signup). */
    private Boolean createdViaSignup;

    /** ZIMRA / tax clearance certificate scan (multipart, same pattern as user national ID upload). */
    private MultipartFile taxClearanceCertificateUpload;
    /** Pre-uploaded file-upload row id when the client uploaded via file-upload-service first. */
    private Long taxClearanceCertificateUploadId;
}
