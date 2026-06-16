package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartSalesOrderDispatchRequest {

    @NotNull
    private Long salesOrderId;

    private Long startedByUserId;
    private Long tripId;
    private Long shipmentId;
}
