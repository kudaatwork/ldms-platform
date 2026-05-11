package projectlx.co.zw.notifications.utils.config;

import com.sendgrid.SendGrid;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class ApiClientConfig {

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    /**
     * Initializes Twilio only when both credentials are set; otherwise SMS/WhatsApp calls fail fast with a clear log.
     */
    @PostConstruct
    public void initTwilio() {
        if (!StringUtils.hasText(twilioAccountSid) || !StringUtils.hasText(twilioAuthToken)) {
            log.warn("Twilio is not initialized: configure twilio.account-sid and twilio.auth-token (or env equivalents) for SMS/WhatsApp.");
            return;
        }
        Twilio.init(twilioAccountSid, twilioAuthToken);
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${sendgrid.api-key:}')")
    public SendGrid sendGridClient(@Value("${sendgrid.api-key}") String sendgridApiKey) {
        return new SendGrid(sendgridApiKey);
    }
}
