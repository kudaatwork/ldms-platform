package projectlx.co.zw.shared_library.billing;

public class InsufficientPlatformWalletBalanceException extends RuntimeException {

    public InsufficientPlatformWalletBalanceException(String message) {
        super(message);
    }
}
