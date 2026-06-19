package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFleetDriverSignupRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseClass;
    private String nationalIdNumber;
    private Long stagingSessionId;
    private Long nationalIdFrontUploadId;
    private Long nationalIdBackUploadId;
    private Long licenseFrontUploadId;
    private Long licenseBackUploadId;

    /**
     * Optional. When blank or null the request type defaults to {@code FREELANCE}.
     * When provided, it is treated as a numeric organisation id (MVP: numeric string = orgId).
     */
    private String companyCode;
}
