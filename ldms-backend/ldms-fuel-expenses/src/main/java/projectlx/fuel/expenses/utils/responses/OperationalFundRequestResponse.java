package projectlx.fuel.expenses.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.fuel.expenses.utils.dtos.OperationalFundRequestDto;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationalFundRequestResponse extends CommonResponse {

    private OperationalFundRequestDto operationalFundRequestDto;
    private List<OperationalFundRequestDto> operationalFundRequestDtoList;
    private long totalElements;
    private int totalPages;
}
