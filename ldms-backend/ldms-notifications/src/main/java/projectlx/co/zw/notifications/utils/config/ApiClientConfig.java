package projectlx.co.zw.notifications.utils.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApiClientConfig {

    private final OutboundMessagingReadiness outboundMessagingReadiness;
    private final OutboundEmailClientSupplier outboundEmailClientSupplier;
    private final OutboundSesClientSupplier outboundSesClientSupplier;
    private final OutboundTwilioInitializer outboundTwilioInitializer;

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
