package projectlx.fuel.expenses.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Feign request sent to ldms-trip-tracking system endpoint to record a trip event.
 * Maps directly to {@code RecordTripEventRequest} in trip-tracking.
 */
@Getter
@Setter
@ToString
public class RecordTripEventFeignRequest {

    private Long tripId;
    private String eventType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
}
