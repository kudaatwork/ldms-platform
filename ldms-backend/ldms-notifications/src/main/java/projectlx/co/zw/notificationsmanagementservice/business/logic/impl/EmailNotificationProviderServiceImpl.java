package projectlx.co.zw.notificationsmanagementservice.business.logic.impl;

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
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@Slf4j
@RequiredArgsConstructor
public class EmailNotificationProviderServiceImpl implements NotificationProviderService {

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;
    private final SesClient sesClient; // Injected by Spring Cloud AWS

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Override
    public Channel getChannel() {
        return Channel.EMAIL;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String recipientEmail = request.getRecipient().getEmail();

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[NOTIFICATION] Skipped channel=EMAIL eventId={} templateKey={} reason=missing_recipient recipientEmail={}",
                    request.getEventId(), request.getTemplateKey(), recipientEmail);
            return;
        }

        String subject = templateProcessor.process(template.getEmailSubject(), request.getData());
        String body = templateProcessor.process(template.getEmailBodyHtml(), request.getData());

        NotificationLog logEntry = createLogEntry(request, "PENDING", null);
        logEntry.setRenderedContent("Subject: " + subject);
        NotificationLog savedLogEntry = notificationLogServiceAuditable.create(logEntry);

        try {

            Destination destination = Destination.builder().toAddresses(recipientEmail).build();
            Content subjectContent = Content.builder().data(subject).build();
            Content bodyContent = Content.builder().data(body).build();
            Body emailBody = Body.builder().html(bodyContent).build();
            Message message = Message.builder().subject(subjectContent).body(emailBody).build();

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(destination)
                    .message(message)
                    .source(fromEmail)
                    .build();

            log.info("[NOTIFICATION] Attempting send channel=EMAIL provider=AWS_SES eventId={} templateKey={} recipientEmail={}", request.getEventId(), request.getTemplateKey(), recipientEmail);
            SendEmailResponse response = sesClient.sendEmail(emailRequest);

            String messageId = response.messageId();
            logEntry.setStatus("SENT");
            logEntry.setProvider("AWS_SES");
            logEntry.setProviderMessageId(messageId);
            NotificationLog updatedLogEntry = notificationLogServiceAuditable.update(logEntry);
            log.info("[NOTIFICATION] Sent channel=EMAIL provider=AWS_SES eventId={} templateKey={} recipientEmail={} messageId={}", request.getEventId(), request.getTemplateKey(), recipientEmail, messageId);

        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=EMAIL provider=AWS_SES eventId={} templateKey={} recipientEmail={} error={}",
                    request.getEventId(), request.getTemplateKey(), recipientEmail, e.getMessage(), e);
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
            NotificationLog updatedLogEntry = notificationLogServiceAuditable.update(logEntry);
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
