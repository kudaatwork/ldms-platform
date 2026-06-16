package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class IngestTelemetryRequest {

    private String ingestKey;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speedKmh;
    private BigDecimal headingDeg;
    private BigDecimal fuelLevelPct;
    private LocalDateTime recordedAt;
}
