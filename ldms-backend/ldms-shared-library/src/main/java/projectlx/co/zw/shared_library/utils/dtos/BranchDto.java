package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.Gender;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchDto {

    private Long id;
    private String branchName;

    // Address Information
    private Long locationId;

    // Contact Information
    private String phoneNumber;
    private String email;

    // Head Office Flag
    private boolean isHeadOffice;

    // Linked Organization ID (to avoid circular references)
    private Long organizationId;

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

    // Branch Geolocation Metadata
    private Double latitude;
    private Double longitude;

    // Operational Metadata
    private String businessHours;
    private String region;

    // Branch Status
    private EntityStatus entityStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
