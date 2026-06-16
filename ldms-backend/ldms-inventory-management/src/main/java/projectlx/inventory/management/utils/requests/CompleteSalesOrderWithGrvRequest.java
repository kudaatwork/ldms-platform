package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteSalesOrderWithGrvRequest {

    @NotNull
    private Long salesOrderId;

    @NotNull
    private Long receivedByUserId;

    @NotBlank
    private String idempotencyKey;
}
