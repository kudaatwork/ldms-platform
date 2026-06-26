package projectlx.billing.payments.utils.enums;

/**
 * Best-effort outcome of dispatching the receipt email when a wallet deposit is approved.
 * SENT: the receipt notification was published to the messaging queue.
 * NO_EMAIL: no organisation email could be resolved, so nothing was dispatched.
 * FAILED: publishing the notification threw an error.
 */
public enum WalletReceiptEmailStatus {
    SENT,
    NO_EMAIL,
    FAILED
}
