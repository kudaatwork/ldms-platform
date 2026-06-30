package projectlx.trip.tracking.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.trip.tracking.utils.dtos.IncomingDeliveryDto;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncomingDeliveriesResponse extends CommonResponse {
    private List<IncomingDeliveryDto> data;
}
