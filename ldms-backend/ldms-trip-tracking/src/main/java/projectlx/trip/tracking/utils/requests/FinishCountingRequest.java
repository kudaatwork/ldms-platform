package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class FinishCountingRequest {

    /** DRIVER | CUSTOMER | RECEIVER */
    private String actorRole;

    /** Optional counted quantity; set by the actor finishing counting. */
    private BigDecimal countedQuantity;
}
