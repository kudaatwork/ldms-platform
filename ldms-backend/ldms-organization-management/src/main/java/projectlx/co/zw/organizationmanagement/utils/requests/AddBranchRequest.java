package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddBranchRequest {

    private String branchName;
    private String branchCode;
    private Long locationId;
    private String phoneNumber;
    private String email;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean headOffice;
    private String region;
    private String businessHours;
}
