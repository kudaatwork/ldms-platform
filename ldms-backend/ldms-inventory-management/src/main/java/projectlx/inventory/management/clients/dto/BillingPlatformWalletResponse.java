package projectlx.inventory.management.clients.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingPlatformWalletResponse {
    private Boolean success;
    private Boolean isSuccess;
    private Integer statusCode;
    private String message;
}
