package projectlx.user.management.utils.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import projectlx.user.management.model.UserAddressDetails;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditUserRequest {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String gender;
    private String nationalIdNumber;
    private MultipartFile nationalIdUpload;
    private String nationalIdExpiryDate;
    private String passportNumber;
    private MultipartFile passportUpload;
    private String passportExpiryDate;
    private String phoneNumber;
    private String dateOfBirth;

    /** When set, persists the national ID file-upload reference without re-uploading a file. */
    private Long nationalIdUploadId;
    /** When set, persists the passport file-upload reference without re-uploading a file. */
    private Long passportUploadId;

    /**
     * When set ({@code true} / {@code false}), toggles organisation KYC approver eligibility.
     * Bound as {@link String} for reliable multipart form binding. Omit to leave unchanged.
     */
    private String organizationKycApprover;

    /** When provided, creates or updates the user's linked address (same as user create). */
    private UserAddressDetails userAddressDetails;
}
