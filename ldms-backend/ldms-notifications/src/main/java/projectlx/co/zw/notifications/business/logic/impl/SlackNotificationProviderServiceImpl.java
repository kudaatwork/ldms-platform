package projectlx.co.zw.notifications.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SlackNotificationProviderServiceImpl implements NotificationProviderService {

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;
    private final RestTemplate restTemplate;

    @Value("${notifications.providers.slack.webhook-url:}")
    private String defaultWebhookUrl;

    @Override
    public Channel getChannel() {
        return Channel.SLACK;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {
        String webhookUrl = resolveWebhookUrl(request);

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[NOTIFICATION] Skipped channel=SLACK eventId={} templateKey={} reason=missing_webhook",
                    request.getEventId(), request.getTemplateKey());
            return;
        }

        String messageText = templateProcessor.process(resolveMessageTemplate(template), request.getData());
        NotificationLog logEntry = createLogEntry(request, "PENDING", null);
        logEntry.setRenderedContent(messageText);
        notificationLogServiceAuditable.create(logEntry);

        try {
            Map<String, String> payload = Map.of("text", messageText);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("[NOTIFICATION] Attempting send channel=SLACK provider=WEBHOOK eventId={} templateKey={}",
                    request.getEventId(), request.getTemplateKey());
            restTemplate.postForEntity(webhookUrl, new HttpEntity<>(payload, headers), String.class);

            logEntry.setStatus("SENT");
            logEntry.setProvider("SLACK_WEBHOOK");
            notificationLogServiceAuditable.update(logEntry);
            log.info("[NOTIFICATION] Sent channel=SLACK provider=WEBHOOK eventId={} templateKey={}",
                    request.getEventId(), request.getTemplateKey());
        } catch (Exception exception) {
            log.error("[NOTIFICATION] Failed channel=SLACK provider=WEBHOOK eventId={} templateKey={} error={}",
                    request.getEventId(), request.getTemplateKey(), exception.getMessage(), exception);
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(exception.getMessage());
            notificationLogServiceAuditable.update(logEntry);
        }
    }

    private String resolveWebhookUrl(NotificationRequest request) {
        if (request.getRecipient() != null) {
            if (request.getRecipient().getSlackWebhookUrl() != null && !request.getRecipient().getSlackWebhookUrl().isBlank()) {
                return request.getRecipient().getSlackWebhookUrl();
            }
            if (request.getRecipient().getChannelWebhookUrls() != null) {
                String configuredUrl = request.getRecipient().getChannelWebhookUrls().get("SLACK");
                if (configuredUrl != null && !configuredUrl.isBlank()) {
                    return configuredUrl;
                }
            }
        }
        return defaultWebhookUrl;
    }

    private String resolveMessageTemplate(NotificationTemplate template) {
        if (template.getInAppBody() != null && !template.getInAppBody().isBlank()) {
            return template.getInAppBody();
        }
        if (template.getSmsBody() != null && !template.getSmsBody().isBlank()) {
            return template.getSmsBody();
        }
        return template.getDescription();
    }

    private NotificationLog createLogEntry(NotificationRequest request, String status, String errorMessage) {
        NotificationLog logEntry = new NotificationLog();
        logEntry.setRecipientId(request.getRecipient() != null ? request.getRecipient().getUserId() : null);
        logEntry.setTemplateKey(request.getTemplateKey());
        logEntry.setChannel(getChannel());
        logEntry.setStatus(status);
        logEntry.setPayload(request.getData());
        logEntry.setErrorMessage(errorMessage);
        return logEntry;
    }
}
