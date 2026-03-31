package projectlx.user.management.service.utils.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.user.management.service.model.UserAddressDetails;
import projectlx.user.management.service.model.UserPreferencesDetails;
import projectlx.user.management.service.model.UserSecurityDetails;
import projectlx.user.management.service.model.UserTypeDetails;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

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
}
