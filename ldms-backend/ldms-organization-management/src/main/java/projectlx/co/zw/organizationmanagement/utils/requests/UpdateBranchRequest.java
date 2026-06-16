package projectlx.co.zw.organizationmanagement.utils.requests;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateBranchRequest {

    private Long organizationId;

    @Size(max = 200)
    private String branchName;

    @Size(max = 50)
    private String branchCode;

    private Long locationId;

    @Size(max = 50)
    private String phoneNumber;

    @Size(max = 255)
    private String email;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean headOffice;

    private Long parentBranchId;

    private Boolean depot;

    @Size(max = 100)
    private String region;

    @Size(max = 200)
    private String businessHours;

    private Boolean active;
}
