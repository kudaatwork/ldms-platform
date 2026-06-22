package projectlx.user.management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "user")
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class User {

    // =============================
    //  Identification & Basic Info
    // =============================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                      // Unique identifier for the user

    @jakarta.persistence.Column(name = "organization_id")
    private Long organizationId;

    @jakarta.persistence.Column(name = "branch_id")
    private Long branchId;

    /** Admin-portal user eligible to review organisation KYC (must have no organisation assignment). */
    @jakarta.persistence.Column(name = "organization_kyc_approver", nullable = false)
    private boolean organizationKycApprover;

    /** Admin-portal user eligible to be assigned Help &amp; Support / operational issue tickets. */
    @jakarta.persistence.Column(name = "operational_issue_handler", nullable = false)
    private boolean operationalIssueHandler;

    /** Platform-portal organisation user eligible to approve procurement workflow stages. */
    @jakarta.persistence.Column(name = "procurement_approver", nullable = false)
    private boolean procurementApprover;

    /** Platform-portal organisation user eligible to allocate fleet to shipments. */
    @jakarta.persistence.Column(name = "shipment_fleet_allocator", nullable = false)
    private boolean shipmentFleetAllocator;

    /** Platform-portal organisation user eligible to verify customer procurement payments. */
    @jakarta.persistence.Column(name = "billing_approver", nullable = false)
    private boolean billingApprover;

    private String username;              // Unique username for login
    private String email;                 // User's email address
    private String firstName;             // User's first name
    private String lastName;              // User's last name

    @Enumerated(EnumType.STRING)
    private Gender gender;                // User's gender

    private String nationalIdNumber;      // National ID number
    private Long nationalIdUploadId;      // National ID file reference

    private String passportNumber;        // Passport number
    private Long passportUploadId;        // Passport file reference

    private String phoneNumber;           // Phone number
    private Date dateOfBirth;             // Date of birth

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;    // Entity status

    private LocalDateTime createdAt;      // Creation timestamp
    private LocalDateTime updatedAt;      // Update timestamp

    // =============================
    //  Authentication & Security
    // =============================

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserAccount userAccount;      // User account for authentication

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPassword userPassword;    // User password management

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSecurity userSecurity;    // User security settings (2FA, security questions)
    
    private Boolean emailVerified = false;  // Whether the user's email has been verified

    @jakarta.persistence.Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;  // Whether the user's phone number has been verified

    @jakarta.persistence.Column(name = "last_phone_verified_at")
    private LocalDateTime lastPhoneVerifiedAt;  // Timestamp of the most recent phone verification

    /** When true, the user must choose a permanent username and password before using the portal. */
    @jakarta.persistence.Column(name = "must_change_credentials", nullable = false)
    private Boolean mustChangeCredentials = false;

    private String verificationToken;       // Token for email verification
    private String passwordResetToken;        // Token for password reset
    private LocalDateTime passwordResetTokenExpiry; // Token expiration time

    // =============================
    //  Profile Preferences
    // =============================

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences userPreferences;  // User's preferences (language, timezone, etc.)

    // =============================
    //  Address & Group Membership
    // =============================

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_address_id", referencedColumnName = "id")
    private Address address;      // Linked address

    @ManyToOne
    @JoinColumn(name = "user_group_id", referencedColumnName = "id")
    @JsonIgnore
    private UserGroup userGroup;          // Linked user group

    // =============================
    //  User Role / Type
    // =============================

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_type_id", referencedColumnName = "id")
    private UserType userType;            // User type (e.g., Admin, Farmer, Organization Contact)

    // =============================
    //  Lifecycle Hooks
    // =============================

    @PrePersist
    public void create() {
        this.createdAt = LocalDateTime.now();
        this.entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void update() {
        this.updatedAt = LocalDateTime.now();
    }
}
