package projectlx.co.zw.notifications.business.logic.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
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
    private final NotificationLogRecorder notificationLogRecorder;
    private final FirebaseMessaging firebaseMessaging;

    @Override
    public Channel getChannel() {
        return Channel.IN_APP;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String fcmToken = request.getRecipient().getFcmToken();

        if (!StringUtils.hasText(fcmToken)) {
            log.warn("[NOTIFICATION] Skipped channel=IN_APP eventId={} templateKey={} reason=missing_recipient fcmToken={}",
                    request.getEventId(), request.getTemplateKey(), fcmToken);
            notificationLogRecorder.markSkipped(request, Channel.IN_APP, "missing_recipient fcmToken");
            return;
        }

        String title = templateProcessor.process(template.getInAppTitle(), request.getData());
        String body = templateProcessor.process(template.getInAppBody(), request.getData());
        String rendered = "Title: " + title + "\nBody: " + body;

        NotificationLog logEntry = notificationLogRecorder.beginDispatch(request, Channel.IN_APP);
        logEntry.setRenderedContent(rendered);

        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(fcmToken)
                    .build();

            log.info("[NOTIFICATION] Attempting send channel=IN_APP provider=FCM eventId={} templateKey={} fcmToken={}",
                    request.getEventId(), request.getTemplateKey(), fcmToken);
            String responseMessageId = firebaseMessaging.send(message);

            notificationLogRecorder.markSent(logEntry, "FCM", responseMessageId, rendered);
            log.info("[NOTIFICATION] Sent channel=IN_APP provider=FCM eventId={} templateKey={} fcmToken={} messageId={}",
                    request.getEventId(), request.getTemplateKey(), fcmToken, responseMessageId);

        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=IN_APP provider=FCM eventId={} templateKey={} fcmToken={} error={}",
                    request.getEventId(), request.getTemplateKey(), fcmToken, e.getMessage(), e);
            notificationLogRecorder.markFailed(logEntry, e.getMessage());
        }
    }
}
