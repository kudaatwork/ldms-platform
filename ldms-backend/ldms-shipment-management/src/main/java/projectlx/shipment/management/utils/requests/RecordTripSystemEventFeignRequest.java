package projectlx.shipment.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RecordTripSystemEventFeignRequest {

    private Long tripId;
    private String eventType;
    private String notes;
}
