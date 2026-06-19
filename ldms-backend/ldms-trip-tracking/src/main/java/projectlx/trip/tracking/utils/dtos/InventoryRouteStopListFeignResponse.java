package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Feign response wrapper for the inventory-management system route-stop endpoint.
 * Maps {@code logisticsRouteStopDtoList} from the LogisticsRouteStopResponse payload.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryRouteStopListFeignResponse {

    private boolean success;
    private Integer statusCode;
    private String message;
    private List<InventoryRouteStopFeignDto> logisticsRouteStopDtoList;
}
