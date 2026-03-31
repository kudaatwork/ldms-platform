package projectlx.co.zw.notifications.utils.config;

import com.sendgrid.SendGrid;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiClientConfig {

    // Twilio Credentials
    @Value("${twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token}")
    private String twilioAuthToken;

    // SendGrid Credentials
    @Value("${sendgrid.api-key}")
    private String sendgridApiKey;

    /**
     * Initializes the Twilio SDK with credentials when the application starts.
     */
    @PostConstruct
    public void initTwilio() {
        Twilio.init(twilioAccountSid, twilioAuthToken);
    }

    /**
     * Creates a SendGrid client bean that can be injected into services.
     * @return A configured SendGrid instance.
     */
    @Bean
    public SendGrid sendGridClient() {
        return new SendGrid(sendgridApiKey);
    }
}
