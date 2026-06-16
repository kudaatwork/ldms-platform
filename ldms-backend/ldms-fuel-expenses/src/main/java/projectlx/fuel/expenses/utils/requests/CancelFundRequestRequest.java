package projectlx.fuel.expenses.utils.requests;

import lombok.Data;

@Data
public class CancelFundRequestRequest {

    private Long requestId;
    private String cancellationReason;
}
