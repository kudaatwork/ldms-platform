package projectlx.fuel.expenses.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import projectlx.fuel.expenses.utils.enums.FuelReadingType;
import projectlx.fuel.expenses.utils.enums.FuelTelemetrySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuelTelemetryLogDto {

    private Long id;
    private Long fuelSessionId;
    private Long tripId;
    private Long organizationId;
    private Long fleetAssetId;

    private FuelTelemetrySource source;
    private FuelReadingType readingType;

    private BigDecimal fuelLevelPct;
    private BigDecimal fuelLiters;
    private BigDecimal odometerKm;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal distanceDeltaKm;
    private BigDecimal consumedLiters;
    private LocalDateTime recordedAt;
    private String notes;

    private LocalDateTime createdAt;
    private String createdBy;
}
