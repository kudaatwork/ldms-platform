package projectlx.user.authentication.service.utils.requests;

import lombok.Data;

@Data
public class VerifyTwoFactorRequest {

    /** The short-lived MFA challenge token returned in the initial authenticate response. */
    private String mfaChallengeToken;

    /** The 6-digit OTP delivered to the user's registered phone number. */
    private String otp;
}
