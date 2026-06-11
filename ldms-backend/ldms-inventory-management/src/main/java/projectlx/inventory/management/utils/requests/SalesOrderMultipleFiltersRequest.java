package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class SalesOrderMultipleFiltersRequest extends MultipleFiltersRequest {

    private String salesOrderNumber;
    private Long customerId;
    private SalesOrderStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private EntityStatus entityStatus;
    private String searchValue;
}