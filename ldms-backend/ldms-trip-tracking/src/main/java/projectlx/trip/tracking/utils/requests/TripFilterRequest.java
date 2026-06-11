package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TripFilterRequest {

    private Long organizationId;
    private String status;
    private String searchTerm;
    private int page;
    private int size;
}
