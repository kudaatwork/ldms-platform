package projectlx.trip.tracking.utils.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.trip.tracking.utils.dtos.FleetAssetSummaryDto;

/**
 * Feign response wrapper for fleet asset system lookup.
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FleetAssetFeignResponse extends CommonResponse {
    private FleetAssetSummaryDto fleetAssetDto;
}
