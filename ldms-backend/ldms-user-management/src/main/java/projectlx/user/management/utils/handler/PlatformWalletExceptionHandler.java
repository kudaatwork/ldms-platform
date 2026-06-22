package projectlx.user.management.utils.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import projectlx.co.zw.shared_library.billing.InsufficientPlatformWalletBalanceException;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@RestControllerAdvice
public class PlatformWalletExceptionHandler {

    @ExceptionHandler(InsufficientPlatformWalletBalanceException.class)
    public ResponseEntity<CommonResponse> handleInsufficientWallet(InsufficientPlatformWalletBalanceException ex) {
        CommonResponse body = new CommonResponse();
        body.setSuccess(false);
        body.setStatusCode(HttpStatus.PAYMENT_REQUIRED.value());
        body.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }
}
