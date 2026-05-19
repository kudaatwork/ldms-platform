package projectlx.co.zw.notifications.utils.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves outbound SMS/WhatsApp (Twilio) and email (AWS SES) readiness from the live environment
 * on each check so {@code ldms-config-repo/.env} values are not missed when evaluated only at startup.
 */
@Component
@RequiredArgsConstructor
public class OutboundMessagingReadiness {

    private final LdmsConfigRepoSecretsResolver secretsResolver;

    public boolean isTwilioReady() {
        return StringUtils.hasText(secretsResolver.twilioAccountSid())
                && StringUtils.hasText(secretsResolver.twilioAuthToken());
    }

    public boolean isSesReady() {
        return secretsResolver.isSesConfigured();
    }

    /** True when AWS SES credentials and from-address are present (SendGrid not required). */
    public boolean isSesEmailReady() {
        return isSesReady();
    }

    public boolean isSendGridEmailReady() {
        return StringUtils.hasText(secretsResolver.sendgridApiKey())
                && StringUtils.hasText(secretsResolver.awsSesFromEmail());
    }

    public static boolean resolveSesReady(String fromEmail, String accessKey, String secretKey) {
        return StringUtils.hasText(fromEmail)
                && StringUtils.hasText(accessKey)
                && StringUtils.hasText(secretKey);
    }
}
