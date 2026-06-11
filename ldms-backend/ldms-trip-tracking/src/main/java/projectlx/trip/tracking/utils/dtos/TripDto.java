package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.trip.tracking.utils.enums.TripStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripDto {

    private Long id;
    private String tripNumber;
    private Long organizationId;
    private Long shipmentId;
    private Long inventoryTransferId;
    private Long fleetDriverId;
    private Long fleetAssetId;
    private TripStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private Long receiverUserId;
    private String fromWarehouseName;
    private String toWarehouseName;
    private String productName;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<TripEventDto> recentEvents;
}
