package projectlx.co.zw.notifications.business.logic.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.config.LdmsConfigRepoSecretsResolver;
import projectlx.co.zw.notifications.utils.config.OutboundMessagingReadiness;
import projectlx.co.zw.notifications.utils.config.OutboundTwilioInitializer;
import projectlx.co.zw.notifications.business.logic.support.NotificationBillingSupport;
import projectlx.co.zw.shared_library.billing.PlatformWalletActionCodes;

public class WhatsAppNotificationProviderServiceImpl implements NotificationProviderService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationProviderServiceImpl.class);

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogRecorder notificationLogRecorder;
    private final OutboundMessagingReadiness outboundMessagingReadiness;
    private final OutboundTwilioInitializer outboundTwilioInitializer;
    private final LdmsConfigRepoSecretsResolver secretsResolver;
    private final NotificationBillingSupport notificationBillingSupport;

    public WhatsAppNotificationProviderServiceImpl(
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
        return Channel.WHATSAPP;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String recipientPhoneNumber = request.getRecipient().getPhoneNumber();

        if (!StringUtils.hasText(recipientPhoneNumber)) {
            log.warn("[NOTIFICATION] Skipped channel=WHATSAPP eventId={} templateKey={} reason=missing_recipient recipientPhone={}",
                    request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);
            notificationLogRecorder.markSkipped(request, Channel.WHATSAPP, "missing_recipient phone");
            return;
        }
        String fromPhoneNumber = secretsResolver.twilioWhatsappFrom();
        if (!StringUtils.hasText(fromPhoneNumber)) {
            log.warn("[NOTIFICATION] Skipped channel=WHATSAPP eventId={} templateKey={} reason=missing_twilio.whatsapp.from-number",
                    request.getEventId(), request.getTemplateKey());
            notificationLogRecorder.markSkipped(request, Channel.WHATSAPP, "missing_twilio.whatsapp.from-number");
            return;
        }
        if (!outboundMessagingReadiness.isTwilioReady()) {
            log.warn("[NOTIFICATION] Skipped channel=WHATSAPP eventId={} templateKey={} reason=twilio_not_configured",
                    request.getEventId(), request.getTemplateKey());
            notificationLogRecorder.markSkipped(request, Channel.WHATSAPP,
                    "twilio_not_configured (set twilio.account-sid and twilio.auth-token)");
            return;
        }

        if (!notificationBillingSupport.authorizeMessagingCharge(request, PlatformWalletActionCodes.WHATSAPP_COMMAND)) {
            notificationLogRecorder.markSkipped(request, Channel.WHATSAPP, "sms_quota_exhausted_wallet_topup_required");
            return;
        }

        String whatsappTemplateBody = template.getWhatsappBody();
        if (whatsappTemplateBody == null || whatsappTemplateBody.isBlank()) {
            // Backward compatibility for older templates saved before whatsappBody existed.
            whatsappTemplateBody = template.getSmsBody();
        }
        // Keep Twilio markdown/newlines exactly as produced after template substitution.
        String resolvedWhatsappBody = templateProcessor.process(whatsappTemplateBody, request.getData());
        NotificationLog logEntry = notificationLogRecorder.beginDispatch(request, Channel.WHATSAPP);
        logEntry.setRenderedContent(resolvedWhatsappBody);

        try {
            outboundTwilioInitializer.ensureInitialized();
            PhoneNumber to = new PhoneNumber("whatsapp:" + recipientPhoneNumber);
            PhoneNumber from = new PhoneNumber("whatsapp:" + fromPhoneNumber);

            log.info("[NOTIFICATION] Attempting send channel=WHATSAPP provider=TWILIO_WHATSAPP eventId={} templateKey={} recipientPhone={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);

            // CORRECTED: Use the standard creator method, which is the most reliable.
            // This method is for sending freeform messages, suitable for the Twilio Sandbox
            // or for replies within a 24-hour user-initiated session.
            Message message = Message.creator(to, from, resolvedWhatsappBody).create();

            // NOTE FOR PRODUCTION: To send a pre-approved template, you would use this instead:
            /*
            String contentSid = template.getWhatsappTemplateName(); // e.g., "HX..."
            if (contentSid == null || contentSid.isBlank()) {
                throw new IllegalArgumentException("WhatsApp template requires a 'whatsappTemplateName' (Content SID)");
            }
            // You would need a method to convert your `request.getData()` map into the correct
            // comma-separated "key=value" string for the variables.
            // String contentVariables = "1=ValueForPlaceholder1,2=ValueForPlaceholder2";
            Message message = Message.creator(to)
                .setFrom(from)
                .setContentSid(contentSid)
                // .setContentVariables(contentVariables) // Uncomment for production templates
                .create();
            */

            notificationLogRecorder.markSent(logEntry, "TWILIO_WHATSAPP", message.getSid(), resolvedWhatsappBody);
            log.info("[NOTIFICATION] Sent channel=WHATSAPP provider=TWILIO_WHATSAPP eventId={} templateKey={} recipientPhone={} sid={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, message.getSid());

        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=WHATSAPP provider=TWILIO_WHATSAPP eventId={} templateKey={} recipientPhone={} error={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, e.getMessage(), e);
            notificationLogRecorder.markFailed(logEntry, e.getMessage());
        }
    }
}
