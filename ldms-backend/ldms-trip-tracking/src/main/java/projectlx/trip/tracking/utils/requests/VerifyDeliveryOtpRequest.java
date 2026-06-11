package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VerifyDeliveryOtpRequest {

    private Long tripId;
    private String otp;
    private Long receiverUserId;
}
