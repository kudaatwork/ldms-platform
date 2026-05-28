package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.organizationmanagement.model.OrganizationType;
import projectlx.co.zw.shared_library.utils.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Completes organisation profile fields not captured at registration, including verification documents.
 */
@Getter
@Setter
public class UpdateOrganizationRequest {

    private String name;
    private String email;
    private String phoneNumber;
    private Long locationId;
    private OrganizationType organizationType;
    private Long industryId;

    private String contactPersonFirstName;
    private String contactPersonLastName;
    private String contactPersonEmail;
    private String contactPersonPhoneNumber;
    private String contactPersonPosition;
    private Gender contactPersonGender;
    private String contactPersonNationalIdNumber;
    private String contactPersonPassportNumber;
    private String contactPersonDateOfBirth;

    private String registrationNumber;
    private String taxNumber;
    private String representativeNationalIdNumber;
    private String representativePassportNumber;

    private String websiteUrl;
    private String organizationDescription;
    private LocalDateTime incorporationDate;
    private String businessHours;
    private Integer numberOfEmployees;
    private BigDecimal annualRevenueEstimate;
    private String regionsServed;

    private Long subscriptionPlanId;
    private String dataProtectionOfficerContact;
    private Boolean twoFactorAuthenticationEnabled;

    private String linkedInUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String youtubeUrl;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long assignedAccountManagerUserId;

    private MultipartFile logoUpload;
    private Long logoUploadId;

    private MultipartFile registrationCertificateUpload;
    private Long registrationCertificateUploadId;

    private MultipartFile taxClearanceCertificateUpload;
    private Long taxClearanceCertificateUploadId;

    private MultipartFile businessLicenseUpload;
    private Long businessLicenseUploadId;

    private MultipartFile proofOfAddressUpload;
    private Long proofOfAddressUploadId;

    private MultipartFile contactPersonNationalIdUpload;
    private Long contactPersonNationalIdUploadId;

    private MultipartFile contactPersonPassportUpload;
    private Long contactPersonPassportUploadId;

    private MultipartFile industrySpecificLicenseUpload;
    private Long industrySpecificLicenseUploadId;
}
