package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.Gender;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"branches", "agents", "representingAgents", "agentAssignments", "customers", "suppliers",
        "contractedTransporters", "contractingOrganizations", "industry"})
public class Organization {
    private Long id;

    private String name;

    // Address Details
    private Long locationId;

    // Contact Details
    private String email;
    private String phoneNumber;

    private OrganizationType organizationType;

    private OrganizationClassification organizationClassification;

    private Industry industry;

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

    // Logo
    private Long logoUploadId;

    // Whether the organization is verified
    private Boolean isVerified = false;

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

    // Additional Metadata
    private String websiteUrl;
    private String organizationDescription;
    private LocalDateTime incorporationDate;
    private String businessHours;
    private Integer numberOfEmployees;
    private Double annualRevenueEstimate;
    private String regionsServed;

    // Subscription Details
    private Long subscriptionPlanId;

    // Security and Access
    private String dataProtectionOfficerContact;
    private boolean twoFactorAuthenticationEnabled;

    // Social Media
    private String linkedInUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String youtubeUrl;

    // Geolocation Metadata
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Account Manager
    private Long assignedAccountManagerUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;

    // --- Relationships ---

    // Branches
    private List<Branch> branches = new ArrayList<>();

    // Direct Agents (for the whole organization)
    private List<Agent> agents = new ArrayList<>();

    // Agents representing this organization as an external entity
    private List<Agent> representingAgents = new ArrayList<>();

    // Link-entity for richer agent relationships
    private List<AgentOrganization> agentAssignments = new ArrayList<>();

    // Customers registered by this Supplier
    private List<Organization> customers = new ArrayList<>();

    // Suppliers for this Customer
    private List<Organization> suppliers = new ArrayList<>();

    // Contracted Transporters (linked to either Supplier or Customer)
    private List<Organization> contractedTransporters = new ArrayList<>();

    // Organizations that have contracted this transporter
    private List<Organization> contractingOrganizations = new ArrayList<>();
}
