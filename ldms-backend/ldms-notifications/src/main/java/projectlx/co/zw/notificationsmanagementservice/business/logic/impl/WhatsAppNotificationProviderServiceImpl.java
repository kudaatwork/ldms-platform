package projectlx.co.zw.notificationsmanagementservice.business.logic.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import projectlx.co.zw.notificationsmanagementservice.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notificationsmanagementservice.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notificationsmanagementservice.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notificationsmanagementservice.model.Channel;
import projectlx.co.zw.notificationsmanagementservice.model.NotificationLog;
import projectlx.co.zw.notificationsmanagementservice.model.NotificationTemplate;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.NotificationRequest;

@Slf4j
@RequiredArgsConstructor
public class WhatsAppNotificationProviderServiceImpl implements NotificationProviderService {

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;

    @Value("${twilio.whatsapp.from-number}")
    private String fromPhoneNumber;

    @Override
    public Channel getChannel() {
        return Channel.WHATSAPP;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String recipientPhoneNumber = request.getRecipient().getPhoneNumber();

        if (recipientPhoneNumber == null || recipientPhoneNumber.isBlank()) {
            log.warn("[NOTIFICATION] Skipped channel=WHATSAPP eventId={} templateKey={} reason=missing_recipient recipientPhone={}",
                    request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);
            return;
        }

        String bodyForLogging = templateProcessor.process(template.getSmsBody(), request.getData());
        NotificationLog logEntry = createLogEntry(request, "PENDING", null);
        logEntry.setRenderedContent(bodyForLogging);
        notificationLogServiceAuditable.create(logEntry);

        try {
            PhoneNumber to = new PhoneNumber("whatsapp:" + recipientPhoneNumber);
            PhoneNumber from = new PhoneNumber("whatsapp:" + fromPhoneNumber);

            log.info("[NOTIFICATION] Attempting send channel=WHATSAPP provider=TWILIO_WHATSAPP eventId={} templateKey={} recipientPhone={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);

            // CORRECTED: Use the standard creator method, which is the most reliable.
            // This method is for sending freeform messages, suitable for the Twilio Sandbox
            // or for replies within a 24-hour user-initiated session.
            Message message = Message.creator(to, from, bodyForLogging).create();

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

            logEntry.setStatus("SENT");
            logEntry.setProvider("TWILIO_WHATSAPP");
            logEntry.setProviderMessageId(message.getSid());
            notificationLogServiceAuditable.update(logEntry);
            log.info("[NOTIFICATION] Sent channel=WHATSAPP provider=TWILIO_WHATSAPP eventId={} templateKey={} recipientPhone={} sid={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, message.getSid());

        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=WHATSAPP provider=TWILIO_WHATSAPP eventId={} templateKey={} recipientPhone={} error={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, e.getMessage(), e);
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
            notificationLogServiceAuditable.update(logEntry);
        }
    }

    private NotificationLog createLogEntry(NotificationRequest request, String status, String errorMessage) {

        NotificationLog logEntry = new NotificationLog();
        logEntry.setRecipientId(request.getRecipient().getUserId());
        logEntry.setTemplateKey(request.getTemplateKey());
        logEntry.setChannel(getChannel());
        logEntry.setStatus(status);
        logEntry.setPayload(request.getData());
        logEntry.setErrorMessage(errorMessage);
        return logEntry;
    }
}
