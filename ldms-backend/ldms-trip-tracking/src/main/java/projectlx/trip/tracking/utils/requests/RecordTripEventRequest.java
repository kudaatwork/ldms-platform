package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class RecordTripEventRequest {

    private Long tripId;
    private String eventType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
}
