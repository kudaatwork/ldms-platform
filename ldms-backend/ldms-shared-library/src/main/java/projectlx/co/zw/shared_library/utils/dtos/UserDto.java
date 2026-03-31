package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.Gender;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    // =============================
    //  Identification & Basic Info
    // =============================

    private Long id;
    private Long organizationId;
    private Long branchId;
    private Long agentId;

    private String username;
    private String email;
    private String firstName;
    private String lastName;

    private Gender gender;

    private String nationalIdNumber;
    private Long nationalIdUploadId;

    private String passportNumber;
    private Long passportUploadId;

    private String phoneNumber;
    private Date dateOfBirth;

    // =============================
    //  Status & Timestamps
    // =============================

    private EntityStatus entityStatus;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =============================
    //  Relationships (represented by nested DTOs)
    // =============================

    private AddressDto addressDto;
    private UserGroupDto userGroupDto;
    private UserTypeDto userTypeDto;
    private UserPreferencesDto userPreferencesDto;
    private UserAccountDto userAccountDto;
    private UserSecurityDto userSecurityDto;

    // NOTE: Exposing password-related information is a security risk.
    // This DTO only includes non-sensitive data like the last update time.
    private UserPasswordDto userPasswordDto;
}
