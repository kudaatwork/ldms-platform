package projectlx.co.zw.notifications.utils.config;

import com.sendgrid.SendGrid;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Lazily creates SendGrid when configured (optional; AWS SES is the default email provider).
 */
@Component
public class OutboundEmailClientSupplier {

    private final LdmsConfigRepoSecretsResolver secretsResolver;
    private volatile SendGrid sendGrid;

    public OutboundEmailClientSupplier(LdmsConfigRepoSecretsResolver secretsResolver) {
        this.secretsResolver = secretsResolver;
    }

    public SendGrid getIfAvailable() {
        if (sendGrid != null) {
            return sendGrid;
        }
        synchronized (this) {
            if (sendGrid != null) {
                return sendGrid;
            }
            String apiKey = secretsResolver.sendgridApiKey();
            if (!StringUtils.hasText(apiKey)) {
                return null;
            }
            sendGrid = new SendGrid(apiKey.trim());
            return sendGrid;
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(secretsResolver.sendgridApiKey())
                && StringUtils.hasText(fromEmail());
    }

    public String fromEmail() {
        String from = secretsResolver.awsSesFromEmail();
        return from != null ? from : "";
    }
}
