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
import java.time.LocalDate;
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
    private String addressLine1;
    private String addressLine2;
    private String addressPostalCode;
    private Long addressSuburbId;
    private Long addressCityId;
    private String addressCityName;
    private Long addressDistrictId;
    private Long addressProvinceId;
    private Long addressCountryId;

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

    /** When true, organisation both buys and sells; relationships define trading role per partner. */
    private Boolean duplexMode;

    private Long industryId;
    private String industryName;
    private String industryCode;

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
    /** User-management user id for the organisation contact person (portal login). */
    private Long contactPersonUserId;

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

    /** Logo file upload reference (Rust FS / file-upload service). */
    private Long logoUploadId;

    // KYC summary (organization-management service; persisted on organization row)
    private String kycStatus;
    private LocalDateTime submittedAt;
    private Integer currentResubmissionCycle;
    private String stage1ReviewedBy;
    private LocalDateTime stage1ReviewedAt;
    private String stage2ReviewedBy;
    private LocalDateTime stage2ReviewedAt;
    private String lastRejectionReason;
    private Integer resubmissionCount;

    // Verification Info
    /** True when KYC Stage 2 is approved (or legacy manual verification). */
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

    /** Pre-assigned stage-1 KYC reviewer (signup organisations only). */
    private Long assignedStage1ApproverUserId;
    private String assignedStage1ApproverUsername;
    private String assignedStage1ApproverDisplayName;
    /** Pre-assigned stage-2 KYC reviewer (signup organisations only). */
    private Long assignedStage2ApproverUserId;
    private String assignedStage2ApproverUsername;
    private String assignedStage2ApproverDisplayName;
    /** Pre-assigned stage-3 KYC reviewer (signup organisations only). */
    private Long assignedStage3ApproverUserId;
    private String assignedStage3ApproverUsername;
    private String assignedStage3ApproverDisplayName;
    /** Pre-assigned stage-4 KYC reviewer (signup organisations only). */
    private Long assignedStage4ApproverUserId;
    private String assignedStage4ApproverUsername;
    private String assignedStage4ApproverDisplayName;
    /** Pre-assigned stage-5 KYC reviewer (signup organisations only). */
    private Long assignedStage5ApproverUserId;
    private String assignedStage5ApproverUsername;
    private String assignedStage5ApproverDisplayName;

    /** Per-organisation override; null inherits platform default. */
    private Integer kycRequiredApprovalStages;
    /** Resolved stage count (override or platform default). */
    private Integer effectiveKycRequiredApprovalStages;

    // System Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;
    private List<BranchDto> branchDtoList;
    private List<AgentDto> agentDtoList;

    /** Linked customer organisations (e.g. supplier → customers); non-deleted only when populated server-side. */
    private List<OrganizationDto> customerDtoList;

    /** Contracted transporter organisations linked to this organisation. */
    private List<OrganizationDto> contractedTransporterDtoList;

    /** Populated when listing/getting a transport partner relationship. */
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private LocalDateTime contractLinkedAt;

    /** Contracted clearing agent organisations linked to this supplier. */
    private List<OrganizationDto> contractedClearingAgentDtoList;
}