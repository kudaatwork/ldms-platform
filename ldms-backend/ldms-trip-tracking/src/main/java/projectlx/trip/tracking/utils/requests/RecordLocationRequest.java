package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class RecordLocationRequest {

    private Long tripId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
