package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateMyOrganizationRequest {

    private String name;
    private String phoneNumber;
    private Long locationId;
    private String websiteUrl;
    private String organizationDescription;
    private String businessHours;
    private Integer numberOfEmployees;
    private BigDecimal annualRevenueEstimate;
    private String regionsServed;
}
