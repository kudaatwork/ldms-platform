package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Lightweight projection of a LogisticsRouteStop returned from the inventory-management
 * system endpoint. Only the fields needed for route planning are mapped.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryRouteStopFeignDto {

    private Integer stopSequence;
    private String stopType;
    private Long warehouseLocationId;
    private String locationLabel;
}
