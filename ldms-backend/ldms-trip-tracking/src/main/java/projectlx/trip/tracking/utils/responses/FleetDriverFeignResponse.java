package projectlx.trip.tracking.utils.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.trip.tracking.utils.dtos.FleetDriverSummaryDto;

/**
 * Feign response wrapper for fleet driver system lookup
 * ({@code GET /ldms-fleet-management/v1/system/fleet-driver/find-by-id/{id}}).
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FleetDriverFeignResponse extends CommonResponse {
    private FleetDriverSummaryDto fleetDriverDto;
}
