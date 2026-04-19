package projectlx.co.zw.organizationmanagement.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(name = "location_id")
    private Long locationId;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", length = 50)
    private OrganizationType organizationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_classification", nullable = false, length = 50)
    private OrganizationClassification organizationClassification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id")
    private Industry industry;

    @Column(name = "contact_person_first_name", length = 100)
    private String contactPersonFirstName;

    @Column(name = "contact_person_last_name", length = 100)
    private String contactPersonLastName;

    @Column(name = "contact_person_email")
    private String contactPersonEmail;

    @Column(name = "contact_person_phone_number", length = 50)
    private String contactPersonPhoneNumber;

    @Column(name = "contact_person_position", length = 100)
    private String contactPersonPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_person_gender", length = 50)
    private Gender contactPersonGender;

    @Column(name = "contact_person_national_id_number", length = 100)
    private String contactPersonNationalIdNumber;

    @Column(name = "contact_person_national_id_upload_id")
    private Long contactPersonNationalIdUploadId;

    @Column(name = "contact_person_passport_number", length = 100)
    private String contactPersonPassportNumber;

    @Column(name = "contact_person_passport_upload_id")
    private Long contactPersonPassportUploadId;

    @Column(name = "contact_person_date_of_birth", length = 20)
    private String contactPersonDateOfBirth;

    @Column(name = "logo_upload_id")
    private Long logoUploadId;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "created_via_signup")
    private Boolean createdViaSignup;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(name = "registration_certificate_upload_id")
    private Long registrationCertificateUploadId;

    @Column(name = "tax_clearance_certificate_upload_id")
    private Long taxClearanceCertificateUploadId;

    @Column(name = "business_license_upload_id")
    private Long businessLicenseUploadId;

    @Column(name = "proof_of_address_upload_id")
    private Long proofOfAddressUploadId;

    @Column(name = "representative_national_id_number", length = 100)
    private String representativeNationalIdNumber;

    @Column(name = "representative_passport_number", length = 100)
    private String representativePassportNumber;

    @Column(name = "industry_specific_license_upload_id")
    private Long industrySpecificLicenseUploadId;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "organization_description", columnDefinition = "TEXT")
    private String organizationDescription;

    @Column(name = "incorporation_date")
    private LocalDateTime incorporationDate;

    @Column(name = "business_hours", length = 200)
    private String businessHours;

    @Column(name = "number_of_employees")
    private Integer numberOfEmployees;

    @Column(name = "annual_revenue_estimate", precision = 19, scale = 4)
    private BigDecimal annualRevenueEstimate;

    @Column(name = "regions_served", length = 500)
    private String regionsServed;

    @Column(name = "subscription_plan_id")
    private Long subscriptionPlanId;

    @Column(name = "data_protection_officer_contact")
    private String dataProtectionOfficerContact;

    @Column(name = "two_factor_authentication_enabled", nullable = false)
    private boolean twoFactorAuthenticationEnabled;

    @Column(name = "linked_in_url", length = 500)
    private String linkedInUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "twitter_url", length = 500)
    private String twitterUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 6)
    private BigDecimal longitude;

    @Column(name = "assigned_account_manager_user_id")
    private Long assignedAccountManagerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 50)
    private KycStatus kycStatus = KycStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "current_resubmission_cycle", nullable = false)
    private int currentResubmissionCycle;

    @Column(name = "stage1_reviewed_by", length = 150)
    private String stage1ReviewedBy;

    @Column(name = "stage1_reviewed_at")
    private LocalDateTime stage1ReviewedAt;

    @Column(name = "stage2_reviewed_by", length = 150)
    private String stage2ReviewedBy;

    @Column(name = "stage2_reviewed_at")
    private LocalDateTime stage2ReviewedAt;

    @Column(name = "last_rejection_reason", length = 1000)
    private String lastRejectionReason;

    @Column(name = "resubmission_count", nullable = false)
    private int resubmissionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Branch> branches = new ArrayList<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Agent> agents = new ArrayList<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrganizationKycReview> kycReviews = new ArrayList<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "organization_customer_supplier",
            joinColumns = @JoinColumn(name = "supplier_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    private List<Organization> customers = new ArrayList<>();

    @ManyToMany(mappedBy = "customers")
    private List<Organization> suppliers = new ArrayList<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "organization_contracted_transporters",
            joinColumns = @JoinColumn(name = "organization_id"),
            inverseJoinColumns = @JoinColumn(name = "transporter_id")
    )
    private List<Organization> contractedTransporters = new ArrayList<>();

    @ManyToMany(mappedBy = "contractedTransporters")
    private List<Organization> contractingOrganizations = new ArrayList<>();
}
