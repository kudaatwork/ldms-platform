package projectlx.co.zw.shared_library.utils.requests.clients;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.model.clients.UserAddressDetails;
import projectlx.co.zw.shared_library.model.clients.UserPreferencesDetails;
import projectlx.co.zw.shared_library.model.clients.UserSecurityDetails;
import projectlx.co.zw.shared_library.model.clients.UserTypeDetails;

@Getter
@Setter
@ToString
public class CreateUserRequest {

    // ===========================
    //  Organization Association
    // ===========================

    private Long organizationId; // Optional link to an organization (if applicable)
    private Long branchId;       // Optional link to a branch (if applicable)
    private Long agentId;        // Optional link to an agent (if applicable)

    private Long locationId;

    // ===========================
    //  Identification & Basic Info
    // ===========================

    private String username;         // Unique username
    private String email;            // Email address
    private String firstName;        // First name
    private String lastName;         // Last name
    private String gender;           // Gender
    private String dateOfBirth;      // Date of birth
    private String phoneNumber;      // Phone number

    // ===========================
    //  Identification Documents
    // ===========================

    private String nationalIdNumber;             // National ID number
    private MultipartFile nationalIdUpload;     // National ID document upload
    private Long nationalIdUploadId;
    private String nationalIdExpiryDate;        // National ID expiry date (optional)

    private String passportNumber;              // Passport number
    private MultipartFile passportUpload;       // Passport document upload
    private Long passportUploadId;
    private String passportExpiryDate;         // Passport expiry date (optional)

    // ===========================
    //  Authentication
    // ===========================

    private String password; // Initial account password

    // ===========================
    //  User Metadata & Configurations
    // ===========================

    private UserTypeDetails userTypeDetails;           // User role or type (e.g., Admin, Farmer)
    private UserAddressDetails userAddressDetails;     // Address information
    private UserPreferencesDetails userPreferencesDetails; // User preferences (language, timezone, etc.)
    private UserSecurityDetails userSecurityDetails;   // Security settings (2FA, security questions, etc.)
}
