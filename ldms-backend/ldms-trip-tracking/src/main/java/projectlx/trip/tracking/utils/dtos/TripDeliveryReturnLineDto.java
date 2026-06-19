package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class TripDeliveryReturnLineDto {

    private Long id;
    private Long workflowId;
    private String productName;
    private BigDecimal quantity;
    private String reason;
    private String recordedByRole;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
}
