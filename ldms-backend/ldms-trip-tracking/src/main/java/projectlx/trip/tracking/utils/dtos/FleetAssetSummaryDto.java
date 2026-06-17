package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FleetAssetSummaryDto {
    private Long id;
    private Long organizationId;
    private String registration;
    private String makeModel;
    private String driverName;
    private Long fleetDriverId;
    private BigDecimal maxSpeedKmh;
}
