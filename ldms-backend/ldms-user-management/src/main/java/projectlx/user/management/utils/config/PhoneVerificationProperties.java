package projectlx.user.management.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ldms.phone-verification")
public class PhoneVerificationProperties {

    /** When false, OTP SMS is not sent and phone verification requests return a clear error. */
    private boolean smsEnabled = true;
}
