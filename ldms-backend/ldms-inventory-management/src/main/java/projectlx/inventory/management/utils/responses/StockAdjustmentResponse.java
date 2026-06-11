package projectlx.inventory.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.inventory.management.utils.dtos.StockAdjustmentDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockAdjustmentResponse extends CommonResponse {
    private StockAdjustmentDto stockAdjustmentDto;
    private List<StockAdjustmentDto> stockAdjustmentDtoList;
    private Page<StockAdjustmentDto> stockAdjustmentDtoPage;
}
