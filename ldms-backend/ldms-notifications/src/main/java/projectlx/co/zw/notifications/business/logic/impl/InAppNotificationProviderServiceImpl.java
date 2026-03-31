package projectlx.co.zw.notifications.business.logic.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;

@Slf4j
@RequiredArgsConstructor
public class InAppNotificationProviderServiceImpl implements NotificationProviderService {

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;
    private final FirebaseMessaging firebaseMessaging; // Inject the FCM bean

    @Override
    public Channel getChannel() {
        return Channel.IN_APP;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String fcmToken = request.getRecipient().getFcmToken();

        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("[NOTIFICATION] Skipped channel=IN_APP eventId={} templateKey={} reason=missing_recipient fcmToken={}",
                    request.getEventId(), request.getTemplateKey(), fcmToken);
            return;
        }

        String title = templateProcessor.process(template.getInAppTitle(), request.getData());
        String body = templateProcessor.process(template.getInAppBody(), request.getData());

        NotificationLog logEntry = createLogEntry(request, "PENDING", null);
        logEntry.setRenderedContent("Title: " + title + "\nBody: " + body);
        notificationLogServiceAuditable.create(logEntry);

        try {
            // Build the FCM message
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    // You can also add custom data to the payload
                    // .putAllData(request.getData())
                    .setToken(fcmToken)
                    .build();

            log.info("[NOTIFICATION] Attempting send channel=IN_APP provider=FCM eventId={} templateKey={} fcmToken={}", request.getEventId(), request.getTemplateKey(), fcmToken);
            String responseMessageId = firebaseMessaging.send(message);

            logEntry.setStatus("SENT");
            logEntry.setProvider("FCM");
            logEntry.setProviderMessageId(responseMessageId);
            notificationLogServiceAuditable.update(logEntry);
            log.info("[NOTIFICATION] Sent channel=IN_APP provider=FCM eventId={} templateKey={} fcmToken={} messageId={}", request.getEventId(), request.getTemplateKey(), fcmToken, responseMessageId);

        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=IN_APP provider=FCM eventId={} templateKey={} fcmToken={} error={}", request.getEventId(), request.getTemplateKey(), fcmToken, e.getMessage(), e);
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
