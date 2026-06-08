package projectlx.user.management.business.logic.support;

/** Raised when SMS OTP cannot be sent because SMS delivery is disabled or unavailable. */
public class SmsDeliveryDisabledException extends RuntimeException {

    public SmsDeliveryDisabledException(String message) {
        super(message);
    }
}
