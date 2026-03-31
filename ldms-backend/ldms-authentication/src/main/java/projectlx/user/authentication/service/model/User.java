package projectlx.user.authentication.service.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.Gender;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class User {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String nationalIdNumber;

    private byte[] nationalIdUploadUrl;
    private String nationalIdName;
    private String phoneNumber;
    private Date dateOfBirth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    private UserAccount userAccount;
    private Address address;
    private UserGroup userGroup;
    private UserPassword userPassword;
    private UserPreferences userPreferences;
    private UserSecurity userSecurity;
    private UserType userType;
}