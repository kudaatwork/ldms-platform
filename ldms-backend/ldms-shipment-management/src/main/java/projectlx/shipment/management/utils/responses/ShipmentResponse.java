package projectlx.shipment.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.shipment.management.utils.dtos.ShipmentDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentResponse extends CommonResponse {
    private ShipmentDto shipmentDto;
    private List<ShipmentDto> shipmentDtoList;
}
