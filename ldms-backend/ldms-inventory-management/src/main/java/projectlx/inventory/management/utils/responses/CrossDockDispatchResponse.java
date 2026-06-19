package projectlx.inventory.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.utils.dtos.CrossDockDispatchDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrossDockDispatchResponse extends CommonResponse {
    private CrossDockDispatchDto crossDockDispatchDto;
    private List<CrossDockDispatchDto> crossDockDispatchDtoList;
    private String shipmentNumber;
    private Long dispatchId;
}
