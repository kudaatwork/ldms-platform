package projectlx.trip.tracking.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.trip.tracking.utils.dtos.TripDeliveryWorkflowDto;
import projectlx.trip.tracking.utils.dtos.TripDto;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripDeliveryWorkflowResponse extends CommonResponse {
    private TripDeliveryWorkflowDto workflowDto;
    private TripDto tripDto;
}
