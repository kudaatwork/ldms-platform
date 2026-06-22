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
    /**
     * When {@code true}, returns trips in any non-terminal active status (in transit, at border,
     * arrived, OTP pending, etc.) regardless of {@link #status}.
     */
    private Boolean activeOnly;
    private String searchTerm;
    private int page;
    private int size;
}
