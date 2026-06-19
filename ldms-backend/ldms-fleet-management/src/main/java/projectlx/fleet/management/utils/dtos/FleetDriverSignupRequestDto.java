package projectlx.fleet.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetDriverSignupRequestDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseClass;
    private String nationalIdNumber;
    private String companyCode;
    private String signupType;
    private String status;
    private Long organizationId;
    private Long stagingSessionId;
    private Long nationalIdFrontUploadId;
    private Long nationalIdBackUploadId;
    private Long licenseFrontUploadId;
    private Long licenseBackUploadId;
    private LocalDateTime createdAt;
}
