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
