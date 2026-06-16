package projectlx.fuel.expenses.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import projectlx.fuel.expenses.utils.enums.FundRequestStatus;
import projectlx.fuel.expenses.utils.enums.FundRequestType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationalFundRequestDto {

    private Long id;
    private String requestNumber;

    private Long tripId;
    private Long organizationId;
    private Long fleetDriverId;
    private Long fleetAssetId;

    private FundRequestType requestType;
    private FundRequestStatus status;

    private BigDecimal litersRequested;
    private BigDecimal amountRequested;
    private String currencyCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String driverNotes;

    private BigDecimal approvedLiters;
    private BigDecimal approvedAmount;
    private String rejectionReason;
    private String decidedBy;
    private LocalDateTime decidedAt;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
