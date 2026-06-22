package projectlx.co.zw.shared_library.billing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformWalletChargeResponse {
    private Boolean success;
    private Boolean isSuccess;
    private Integer statusCode;
    private String message;
}
