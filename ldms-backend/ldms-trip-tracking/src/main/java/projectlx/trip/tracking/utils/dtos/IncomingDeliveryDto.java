package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** A trip inbound to the clerk's organisation, shown in the clerk workspace. */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncomingDeliveryDto {
    private Long tripId;
    private String tripNumber;
    private String status;
    private String productName;
    private BigDecimal quantity;
    private String driverName;
    private String vehicleReg;
    private String originBranch;
    private String eta;
    private String arrivedAt;
    private Long inventoryTransferId;
}
