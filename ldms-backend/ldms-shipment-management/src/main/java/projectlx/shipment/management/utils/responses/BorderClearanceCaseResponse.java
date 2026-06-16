package projectlx.shipment.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.shipment.management.utils.dtos.BorderClearanceCaseDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BorderClearanceCaseResponse extends CommonResponse {

    private BorderClearanceCaseDto borderClearanceCaseDto;
    private List<BorderClearanceCaseDto> borderClearanceCaseDtoList;
}
