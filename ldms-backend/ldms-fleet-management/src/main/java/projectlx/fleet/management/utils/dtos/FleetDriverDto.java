package projectlx.fleet.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FleetDriverDto {

    private Long id;
    private Long organizationId;
    private Long userId;
    private String employmentType;
    private Boolean marketplaceVisible;

    /** Populated when listing a contracted partner's roster (cross-org read). */
    private String rosterSource;
    private String homeOrganizationName;

    // Personal details
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseClass;
    private Long licenseUploadId;
    private Long licenseBackUploadId;

    // Identity documents
    private String nationalIdNumber;
    private LocalDate nationalIdExpiryDate;
    private Long nationalIdUploadId;
    private Long nationalIdBackUploadId;
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

    // Audit
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
