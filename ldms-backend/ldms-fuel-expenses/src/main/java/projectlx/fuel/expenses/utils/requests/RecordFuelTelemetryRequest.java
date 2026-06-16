package projectlx.fuel.expenses.utils.requests;

import lombok.Data;
import projectlx.fuel.expenses.utils.enums.FuelReadingType;
import projectlx.fuel.expenses.utils.enums.FuelTelemetrySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecordFuelTelemetryRequest {

    private Long tripId;
    private Long fleetAssetId;

    private FuelTelemetrySource source;
    private FuelReadingType readingType;

    private BigDecimal fuelLevelPct;
    private BigDecimal fuelLiters;
    private BigDecimal odometerKm;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;

    /** When the reading was physically taken — defaults to now if null. */
    private LocalDateTime recordedAt;
}
