package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class CreateFleetDriverRequest {

    private Long userId;

    /**
     * Optional email address.
     * Required when {@code provisionPlatformAccess=true} so the user account can be created.
     */
    private String email;

    /**
     * When {@code true} and no {@code userId} is supplied, a platform user account will be
     * provisioned for the driver and temporary credentials returned in the response.
     */
    private Boolean provisionPlatformAccess;

    // Personal details
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseClass;
    private Long licenseUploadId;

    // Identity documents
    private String nationalIdNumber;
    private LocalDate nationalIdExpiryDate;
    private Long nationalIdUploadId;
    private String passportNumber;
    private LocalDate passportExpiryDate;
    private Long passportUploadId;

    // Residential address
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressProvince;
    private String addressPostalCode;
    private String addressCountry;

    /** EMPLOYED or POOL — defaults to EMPLOYED when linked to org user. */
    private String employmentType;
}
