package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.Gender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class Branch {
    private Long id;

    private String branchName;

    // Address Information
    private Long locationId;

    // Contact Information
    private String phoneNumber;
    private String email;

    // Head Office Flag
    private boolean isHeadOffice = false;

    // Linked Organization
    private Organization organization;

    // Branch Manager Information
    private String managerFirstName;
    private String managerLastName;
    private String managerEmail;
    private String managerPhoneNumber;
    private Gender managerGender;
    private String managerNationalIdNumber;
    private String managerPassportNumber;
    private String managerDateOfBirth;
    private Long managerUserId;

    // Operational Metadata
    private String businessHours;  // e.g., "Mon-Fri 8am-5pm"
    private String region;         // e.g., "Southern Region", "Harare"

    // Branch Status
    private EntityStatus entityStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional Agents Linked to Branch (if business rules allow it)
    private List<Agent> branchAgents = new ArrayList<>();
}