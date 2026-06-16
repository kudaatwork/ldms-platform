package projectlx.fuel.expenses.utils.requests;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApproveFundRequestRequest {

    private Long requestId;

    /** Approved litres for FUEL_TOP_UP — may differ from originally requested amount. */
    private BigDecimal approvedLiters;

    /** Approved cash amount for FUNDS requests. */
    private BigDecimal approvedAmount;
}
