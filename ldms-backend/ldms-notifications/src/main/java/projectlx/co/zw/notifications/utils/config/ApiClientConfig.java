package projectlx.co.zw.notifications.utils.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(ApiClientConfig.class);

    private final OutboundMessagingReadiness outboundMessagingReadiness;
    private final OutboundEmailClientSupplier outboundEmailClientSupplier;
    private final OutboundSesClientSupplier outboundSesClientSupplier;
    private final OutboundTwilioInitializer outboundTwilioInitializer;

    public ApiClientConfig(
            OutboundMessagingReadiness outboundMessagingReadiness,
            OutboundEmailClientSupplier outboundEmailClientSupplier,
            OutboundSesClientSupplier outboundSesClientSupplier,
            OutboundTwilioInitializer outboundTwilioInitializer) {
        this.outboundMessagingReadiness = outboundMessagingReadiness;
        this.outboundEmailClientSupplier = outboundEmailClientSupplier;
        this.outboundSesClientSupplier = outboundSesClientSupplier;
        this.outboundTwilioInitializer = outboundTwilioInitializer;
    }

    /**
     * Warms Twilio when credentials are already available at startup; sends also call
     * {@link OutboundTwilioInitializer#ensureInitialized()} lazily after config-repo secrets load.
     */
    @PostConstruct
    public void initTwilio() {
        if (!outboundMessagingReadiness.isTwilioReady()) {
            log.warn("Twilio is not initialized: configure twilio.account-sid and twilio.auth-token (or env equivalents) for SMS/WhatsApp.");
            return;
        }
        outboundTwilioInitializer.ensureInitialized();
    }

    @PostConstruct
    public void logEmailOutboundReadiness() {
        boolean sesReady = outboundMessagingReadiness.isSesEmailReady();
        boolean sesClientOk = outboundSesClientSupplier.getIfAvailable() != null;

        log.info(
                "[NOTIFICATION] Email outbound readiness (default=AWS SES): sesReady={} sesClientOk={} sendgridConfigured={}",
                sesReady,
                sesClientOk,
                outboundEmailClientSupplier.isConfigured());

        if (!sesReady) {
            log.warn(
                    "[NOTIFICATION] AWS SES is not ready. Set in ldms-config-repo/.env: "
                            + "AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SES_FROM_EMAIL, AWS_REGION. "
                            + "SendGrid is optional and not required when using SES.");
        } else if (!sesClientOk) {
            log.warn("[NOTIFICATION] AWS SES credentials look valid but SesClient could not be created; check AWS_REGION and IAM permissions.");
        }
    }
}
