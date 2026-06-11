package projectlx.billing.payments.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.billing.payments.utils.dtos.DriverExpenseReconciliationDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriverExpenseResponse extends CommonResponse {
    private DriverExpenseReconciliationDto driverExpenseReconciliationDto;
    private List<DriverExpenseReconciliationDto> driverExpenseReconciliationDtoList;
}
