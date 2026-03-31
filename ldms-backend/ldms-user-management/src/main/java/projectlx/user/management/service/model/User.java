package projectlx.user.management.service.model;

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
@Table
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

    private Long organizationId;          // Organization ID for the user
    private Long branchId;                // Branch ID for the user
    private Long agentId;                 // Agent ID for the user

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

    @ManyToOne(cascade = CascadeType.ALL)
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
