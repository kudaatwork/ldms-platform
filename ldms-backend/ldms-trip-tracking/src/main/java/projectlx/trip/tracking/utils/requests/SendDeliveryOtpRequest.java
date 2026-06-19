package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SendDeliveryOtpRequest {

    private Long tripId;

    /** SMS | WHATSAPP | EMAIL */
    private String channel;

    /** Phone number or email address to send the OTP to. */
    private String recipientContact;

    /** Optional: platform user id of the recipient (may be null). */
    private Long recipientUserId;
}
