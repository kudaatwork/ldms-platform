package projectlx.co.zw.notifications.business.logic.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.business.logic.support.NotificationBillingSupport;
import projectlx.co.zw.shared_library.billing.PlatformWalletActionCodes;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.config.LdmsConfigRepoSecretsResolver;
import projectlx.co.zw.notifications.utils.config.OutboundMessagingReadiness;
import projectlx.co.zw.notifications.utils.config.OutboundTwilioInitializer;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;

public class SmsNotificationProviderServiceImpl implements NotificationProviderService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationProviderServiceImpl.class);

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogRecorder notificationLogRecorder;
    private final OutboundMessagingReadiness outboundMessagingReadiness;
    private final OutboundTwilioInitializer outboundTwilioInitializer;
    private final LdmsConfigRepoSecretsResolver secretsResolver;
    private final NotificationBillingSupport notificationBillingSupport;

    public SmsNotificationProviderServiceImpl(
            TemplateProcessorService templateProcessor,
            NotificationLogRecorder notificationLogRecorder,
            OutboundMessagingReadiness outboundMessagingReadiness,
            OutboundTwilioInitializer outboundTwilioInitializer,
            LdmsConfigRepoSecretsResolver secretsResolver,
            NotificationBillingSupport notificationBillingSupport) {
        this.templateProcessor = templateProcessor;
        this.notificationLogRecorder = notificationLogRecorder;
        this.outboundMessagingReadiness = outboundMessagingReadiness;
        this.outboundTwilioInitializer = outboundTwilioInitializer;
        this.secretsResolver = secretsResolver;
        this.notificationBillingSupport = notificationBillingSupport;
    }

    @Override
    public Channel getChannel() {
        return Channel.SMS;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String recipientPhoneNumber = request.getRecipient().getPhoneNumber();

        if (!StringUtils.hasText(recipientPhoneNumber)) {
            log.warn("[NOTIFICATION] Skipped channel=SMS eventId={} templateKey={} reason=missing_recipient recipientPhone={}",
                    request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);
            notificationLogRecorder.markSkipped(request, Channel.SMS, "missing_recipient phone");
            return;
        }
        String fromPhoneNumber = secretsResolver.twilioPhoneNumber();
        if (!StringUtils.hasText(fromPhoneNumber)) {
            log.warn("[NOTIFICATION] Skipped channel=SMS eventId={} templateKey={} reason=missing_twilio.phone-number",
                    request.getEventId(), request.getTemplateKey());
            notificationLogRecorder.markSkipped(request, Channel.SMS, "missing_twilio.phone-number");
            return;
        }
        if (!outboundMessagingReadiness.isTwilioReady()) {
            log.warn("[NOTIFICATION] Skipped channel=SMS eventId={} templateKey={} reason=twilio_not_configured",
                    request.getEventId(), request.getTemplateKey());
            notificationLogRecorder.markSkipped(request, Channel.SMS,
                    "twilio_not_configured (set twilio.account-sid and twilio.auth-token)");
            return;
        }

        if (!notificationBillingSupport.authorizeMessagingCharge(request, PlatformWalletActionCodes.NOTIFICATION_SMS)) {
            notificationLogRecorder.markSkipped(request, Channel.SMS, "sms_quota_exhausted_wallet_topup_required");
            return;
        }

        String body = templateProcessor.process(template.getSmsBody(), request.getData());
        NotificationLog logEntry = notificationLogRecorder.beginDispatch(request, Channel.SMS);
        logEntry.setRenderedContent(body);

        try {
            outboundTwilioInitializer.ensureInitialized();
            PhoneNumber to = new PhoneNumber(recipientPhoneNumber);
            PhoneNumber from = new PhoneNumber(fromPhoneNumber);

            log.info("[NOTIFICATION] Attempting send channel=SMS provider=TWILIO eventId={} templateKey={} recipientPhone={}",
                    request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);
            Message message = Message.creator(to, from, body).create();

            notificationLogRecorder.markSent(logEntry, "TWILIO", message.getSid(), body);
            log.info("[NOTIFICATION] Sent channel=SMS provider=TWILIO eventId={} templateKey={} recipientPhone={} sid={}",
                    request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, message.getSid());

        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=SMS provider=TWILIO eventId={} templateKey={} recipientPhone={} error={}",
                    request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, e.getMessage(), e);
            notificationLogRecorder.markFailed(logEntry, e.getMessage());
        }
    }
}
