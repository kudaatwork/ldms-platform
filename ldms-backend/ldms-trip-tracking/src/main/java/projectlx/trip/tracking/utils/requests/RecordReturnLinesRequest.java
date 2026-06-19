package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class RecordReturnLinesRequest {

    /** DRIVER | CUSTOMER | RECEIVER */
    private String actorRole;

    private List<ReturnLineItem> returnLines;
}
