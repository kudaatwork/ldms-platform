package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StartCountingRequest {

    /** DRIVER | CUSTOMER | RECEIVER */
    private String actorRole;
}
