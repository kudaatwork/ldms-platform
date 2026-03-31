package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.model.OrganizationClassification;
import projectlx.co.zw.shared_library.model.OrganizationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.Gender;
import projectlx.co.zw.shared_library.utils.enums.VerificationMethod;
import projectlx.co.zw.shared_library.utils.enums.VerificationSource;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationDto {

    private Long id;
    private String name;

    // Address Details
    private Long locationId;

    // Contact Details
    private String email;
    private String phoneNumber;

    // Type & Classification
    public void setOrganizationType(Object type) {
        if (type instanceof OrganizationType) {
            this.organizationType = (OrganizationType) type;
        } else if (type != null) {
            try {
                this.organizationType = OrganizationType.valueOf(type.toString());
            } catch (Exception e) {}
        }
    }

    private OrganizationType organizationType;

    public void setOrganizationClassification(Object classification) {
        if (classification instanceof OrganizationClassification) {
            this.organizationClassification = (OrganizationClassification) classification;
        } else if (classification != null) {
            try {
                this.organizationClassification = OrganizationClassification.valueOf(classification.toString());
            } catch (Exception e) {}
        }
    }

    private OrganizationClassification organizationClassification;
    private Long industryId;

    // Contact Person Details
    private String contactPersonFirstName;
    private String contactPersonLastName;
    private String contactPersonEmail;
    private String contactPersonPhoneNumber;
    private String contactPersonPosition;
    private Gender contactPersonGender;
    private String contactPersonNationalIdNumber;
    private Long contactPersonNationalIdUploadId;
    private String contactPersonPassportNumber;
    private Long contactPersonPassportUploadId;
    private String contactPersonDateOfBirth;

    // Verification Artifacts
    private String registrationNumber;
    private String taxNumber;
    private Long registrationCertificateUploadId;
    private Long taxClearanceCertificateUploadId;
    private Long businessLicenseUploadId;
    private Long proofOfAddressUploadId;
    private String representativeNationalIdNumber;
    private String representativePassportNumber;
    private Long industrySpecificLicenseUploadId;

    // Metadata
    private String websiteUrl;
    private String organizationDescription;
    private LocalDateTime incorporationDate;
    private String businessHours;
    private Integer numberOfEmployees;
    private Double annualRevenueEstimate;
    private String regionsServed;

    // Subscription
    private Long subscriptionPlanId;

    // Security
    private String dataProtectionOfficerContact;
    private boolean twoFactorAuthenticationEnabled;

    // Social Media
    private String linkedInUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String youtubeUrl;

    // Verification Info
    /** True when EntityVerification for this org has verifiedAt set. */
    private Boolean isVerified;
    /** For CUSTOMER only: true when created via signup page (pending approval flow). */
    private Boolean createdViaSignup;
    private boolean manualVerified;
    private Long manualVerifiedByUserId;
    private String manualVerificationNotes;
    private LocalDateTime manualVerifiedAt;
    private VerificationMethod manualVerificationMethod;
    private VerificationSource manualVerificationSource;

    // Geolocation
    private Double latitude;
    private Double longitude;

    // Account Manager
    private Long assignedAccountManagerUserId;

    // System Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;
    private List<BranchDto> branchDtoList;
    private List<AgentDto> agentDtoList;
}