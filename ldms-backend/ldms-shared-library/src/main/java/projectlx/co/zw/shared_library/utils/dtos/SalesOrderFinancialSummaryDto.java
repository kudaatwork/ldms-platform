package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesOrderFinancialSummaryDto {
    private Long salesOrderId;
    private String salesOrderNumber;
    private Long customerId;
    private Long supplierOrganizationId;
    private PaymentTerm paymentTerm;
    private BigDecimal totalAmount;
}
