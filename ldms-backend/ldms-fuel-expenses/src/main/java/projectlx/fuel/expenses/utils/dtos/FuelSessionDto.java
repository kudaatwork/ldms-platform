package projectlx.fuel.expenses.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import projectlx.fuel.expenses.utils.enums.FuelSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuelSessionDto {

    private Long id;
    private Long tripId;
    private Long organizationId;
    private Long fleetAssetId;
    private Long fleetDriverId;
    private Long shipmentId;

    private BigDecimal tankCapacityLiters;
    private BigDecimal fuelRemainingLiters;
    private BigDecimal fuelLevelPct;
    private BigDecimal consumptionRateLPer100km;
    private BigDecimal distanceTravelledKm;

    private BigDecimal lastLatitude;
    private BigDecimal lastLongitude;

    private FuelSessionStatus status;
    private boolean moving;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
