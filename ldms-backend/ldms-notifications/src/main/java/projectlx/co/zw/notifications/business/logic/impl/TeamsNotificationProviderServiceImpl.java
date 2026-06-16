package projectlx.co.zw.notifications.business.logic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;

import java.util.Map;

public class TeamsNotificationProviderServiceImpl implements NotificationProviderService {

    private static final Logger log = LoggerFactory.getLogger(TeamsNotificationProviderServiceImpl.class);

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogRecorder notificationLogRecorder;
    private final RestTemplate restTemplate;

    @Value("${notifications.providers.teams.webhook-url:}")
    private String defaultWebhookUrl;

    public TeamsNotificationProviderServiceImpl(
            TemplateProcessorService templateProcessor,
            NotificationLogRecorder notificationLogRecorder,
            RestTemplate restTemplate) {
        this.templateProcessor = templateProcessor;
        this.notificationLogRecorder = notificationLogRecorder;
        this.restTemplate = restTemplate;
    }

    @Override
    public Channel getChannel() {
        return Channel.TEAMS;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {
        String webhookUrl = resolveWebhookUrl(request);

        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("[NOTIFICATION] Skipped channel=TEAMS eventId={} templateKey={} reason=missing_webhook",
                    request.getEventId(), request.getTemplateKey());
            notificationLogRecorder.markSkipped(request, Channel.TEAMS, "missing_webhook");
            return;
        }

        String messageText = templateProcessor.process(resolveMessageTemplate(template), request.getData());
        NotificationLog logEntry = notificationLogRecorder.beginDispatch(request, Channel.TEAMS);
        logEntry.setRenderedContent(messageText);

        try {
            Map<String, String> payload = Map.of("text", messageText);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("[NOTIFICATION] Attempting send channel=TEAMS provider=WEBHOOK eventId={} templateKey={}",
                    request.getEventId(), request.getTemplateKey());
            restTemplate.postForEntity(webhookUrl, new HttpEntity<>(payload, headers), String.class);

            notificationLogRecorder.markSent(logEntry, "TEAMS_WEBHOOK", null, messageText);
            log.info("[NOTIFICATION] Sent channel=TEAMS provider=WEBHOOK eventId={} templateKey={}",
                    request.getEventId(), request.getTemplateKey());
        } catch (Exception exception) {
            log.error("[NOTIFICATION] Failed channel=TEAMS provider=WEBHOOK eventId={} templateKey={} error={}",
                    request.getEventId(), request.getTemplateKey(), exception.getMessage(), exception);
            notificationLogRecorder.markFailed(logEntry, exception.getMessage());
        }
    }

    private String resolveWebhookUrl(NotificationRequest request) {
        if (request.getRecipient() != null) {
            if (request.getRecipient().getTeamsWebhookUrl() != null && !request.getRecipient().getTeamsWebhookUrl().isBlank()) {
                return request.getRecipient().getTeamsWebhookUrl();
            }
            if (request.getRecipient().getChannelWebhookUrls() != null) {
                String configuredUrl = request.getRecipient().getChannelWebhookUrls().get("TEAMS");
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
}
