package projectlx.co.zw.notifications.utils.config;

import com.twilio.Twilio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Lazily initializes the Twilio SDK with credentials from {@link LdmsConfigRepoSecretsResolver}.
 * {@link com.twilio.Twilio#init} must not run only at {@code @PostConstruct} because ldms-config-repo
 * secrets are often applied after the first environment pass.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboundTwilioInitializer {

    private final LdmsConfigRepoSecretsResolver secretsResolver;
    private final OutboundMessagingReadiness outboundMessagingReadiness;

    private volatile String activeCredentialKey;

    public void ensureInitialized() {
        if (!outboundMessagingReadiness.isTwilioReady()) {
            return;
        }
        String accountSid = secretsResolver.twilioAccountSid().trim();
        String authToken = secretsResolver.twilioAuthToken().trim();
        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken)) {
            return;
        }
        String credentialKey = accountSid + "|" + authToken;
        if (credentialKey.equals(activeCredentialKey)) {
            return;
        }
        synchronized (this) {
            if (credentialKey.equals(activeCredentialKey)) {
                return;
            }
            Twilio.init(accountSid, authToken);
            activeCredentialKey = credentialKey;
            log.info("[NOTIFICATION] Twilio SDK initialized (accountSid suffix={})", suffix(accountSid));
        }
    }

    private static String suffix(String accountSid) {
        if (!StringUtils.hasText(accountSid) || accountSid.length() < 4) {
            return "****";
        }
        return accountSid.substring(accountSid.length() - 4);
    }
}
