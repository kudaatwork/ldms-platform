package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class TripDeliveryWorkflowDto {

    private Long id;
    private Long tripId;
    private String tripNumber;

    // Counting timestamps
    private LocalDateTime driverCountingStartedAt;
    private LocalDateTime driverCountingFinishedAt;
    private LocalDateTime customerCountingStartedAt;
    private LocalDateTime customerCountingFinishedAt;

    // Quantities
    private BigDecimal expectedQuantity;
    private BigDecimal countedQuantity;

    // OTP channel
    private String otpChannel;
    private String otpRecipient;

    // Delivery notes
    private String deliveryNotes;

    // Return journey
    private LocalDateTime returnInitiatedAt;
    private LocalDateTime returnCompletedAt;

    // Return lines
    private List<TripDeliveryReturnLineDto> returnLines;

    // Audit
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
