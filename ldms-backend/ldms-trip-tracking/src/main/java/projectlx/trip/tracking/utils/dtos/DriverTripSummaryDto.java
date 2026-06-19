package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriverTripSummaryDto {

    private Long id;
    private String tripNumber;
    private String shipmentNumber;
    private String route;
    private String cargoLabel;
    private String productName;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private String vehicleRegistration;
    private String status;
    private String statusLabel;
    private String statusTone;
    private String startedAtLabel;
    private String estimatedArrivalLabel;
    private boolean canTriggerArrival;
    private boolean canStartDeliveryWorkflow;
    private boolean canLiveTrack;
    private String deliveryWorkflowPhase;
}
