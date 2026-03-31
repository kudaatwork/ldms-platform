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
import projectlx.co.zw.notificationsmanagementservice.repository.NotificationLogRepository;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.NotificationRequest;

@Slf4j
@RequiredArgsConstructor
public class SmsNotificationProviderServiceImpl implements NotificationProviderService {

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @Override
    public Channel getChannel() {
        return Channel.SMS;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String recipientPhoneNumber = request.getRecipient().getPhoneNumber();

        if (recipientPhoneNumber == null || recipientPhoneNumber.isBlank()) {
            log.warn("[NOTIFICATION] Skipped channel=SMS eventId={} templateKey={} reason=missing_recipient recipientPhone={}",
                    request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);
            return;
        }

        String body = templateProcessor.process(template.getSmsBody(), request.getData());
        NotificationLog logEntry = createLogEntry(request, "PENDING", null);
        logEntry.setRenderedContent(body);
        NotificationLog savedLogEntry = notificationLogServiceAuditable.create(logEntry);

        try {
            PhoneNumber to = new PhoneNumber(recipientPhoneNumber);
            PhoneNumber from = new PhoneNumber(fromPhoneNumber);

            log.info("[NOTIFICATION] Attempting send channel=SMS provider=TWILIO eventId={} templateKey={} recipientPhone={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber);
            Message message = Message.creator(to, from, body).create();

            logEntry.setStatus("SENT");
            logEntry.setProvider("TWILIO");
            logEntry.setProviderMessageId(message.getSid());
            NotificationLog updatedLogEntry = notificationLogServiceAuditable.update(logEntry);
            log.info("[NOTIFICATION] Sent channel=SMS provider=TWILIO eventId={} templateKey={} recipientPhone={} sid={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, message.getSid());

        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=SMS provider=TWILIO eventId={} templateKey={} recipientPhone={} error={}", request.getEventId(), request.getTemplateKey(), recipientPhoneNumber, e.getMessage(), e);
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
            NotificationLog updatedLogEntry = notificationLogServiceAuditable.update(logEntry);
        }
    }

    // Helper method to create a log entry
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


