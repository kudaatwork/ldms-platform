package projectlx.co.zw.notifications.utils.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Logs whether AWS SES / Twilio credentials resolved after the application context is up. */
@Component
public class NotificationSecretsStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private final LdmsConfigRepoSecretsResolver secretsResolver;
    private final OutboundSesClientSupplier outboundSesClientSupplier;

    public NotificationSecretsStartupLogger(
            LdmsConfigRepoSecretsResolver secretsResolver,
            OutboundSesClientSupplier outboundSesClientSupplier) {
        this.secretsResolver = secretsResolver;
        this.outboundSesClientSupplier = outboundSesClientSupplier;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        secretsResolver.logCredentialPresenceAtStartup();
        if (outboundSesClientSupplier.isConfigured() && outboundSesClientSupplier.getIfAvailable() == null) {
            // isConfigured true but client null means build failed — already logged in supplier
        }
    }
}
