package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateMyOrganizationRequest {

    private String name;
    private String email;
    private String phoneNumber;

    // Explicit location reference (takes precedence over address fields below)
    private Long locationId;

    // Address fields — when line1, postalCode, and suburbId are provided a new address row
    // is created in ldms-locations and the resulting locationId is applied to the organisation.
    private String addressLine1;
    private String addressLine2;
    private String postalCode;
    private Long suburbId;
    private Long cityId;

    private String websiteUrl;
    private String organizationDescription;
    private String businessHours;
    private Integer numberOfEmployees;
    private BigDecimal annualRevenueEstimate;
    private String regionsServed;
}
