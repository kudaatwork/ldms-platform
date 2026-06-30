package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Contact details for the trip's receiving party (consignee). */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceiverContactDto {
    private Long userId;
    private String name;
    private String phoneNumber;
    private String email;
    private String destinationName;
    private boolean reachable;
}
