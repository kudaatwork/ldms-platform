package projectlx.co.zw.notifications.business.logic.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class EmailNotificationProviderServiceImpl implements NotificationProviderService {

    private final TemplateProcessorService templateProcessor;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;
    private final ObjectProvider<SesClient> sesClientProvider;
    private final ObjectProvider<SendGrid> sendGridProvider;

    @Value("${aws.ses.from-email:}")
    private String sesFromEmail;

    @Value("${sendgrid.from-email:${aws.ses.from-email:}}")
    private String sendgridFromEmail;

    public EmailNotificationProviderServiceImpl(
            TemplateProcessorService templateProcessor,
            NotificationLogServiceAuditable notificationLogServiceAuditable,
            ObjectProvider<SesClient> sesClientProvider,
            ObjectProvider<SendGrid> sendGridProvider) {
        this.templateProcessor = templateProcessor;
        this.notificationLogServiceAuditable = notificationLogServiceAuditable;
        this.sesClientProvider = sesClientProvider;
        this.sendGridProvider = sendGridProvider;
    }

    @Override
    public Channel getChannel() {
        return Channel.EMAIL;
    }

    @Override
    public void send(NotificationRequest request, NotificationTemplate template) {

        String recipientEmail = request.getRecipient().getEmail();

        if (!StringUtils.hasText(recipientEmail)) {
            log.warn("[NOTIFICATION] Skipped channel=EMAIL eventId={} templateKey={} reason=missing_recipient recipientEmail={}",
                    request.getEventId(), request.getTemplateKey(), recipientEmail);
            return;
        }

        SesClient sesClient = sesClientProvider.getIfAvailable();
        SendGrid sendGrid = sendGridProvider.getIfAvailable();
        boolean canSes = sesClient != null && StringUtils.hasText(sesFromEmail);
        boolean canSendgrid = sendGrid != null && StringUtils.hasText(sendgridFromEmail);
        if (!canSes && !canSendgrid) {
            log.warn("[NOTIFICATION] Skipped channel=EMAIL eventId={} templateKey={} reason=no_email_backend "
                            + "(configure Spring Cloud AWS SES + aws.ses.from-email, or sendgrid.api-key + sendgrid.from-email)",
                    request.getEventId(), request.getTemplateKey());
            return;
        }

        String subject = templateProcessor.process(template.getEmailSubject(), request.getData());
        String body = templateProcessor.process(template.getEmailBodyHtml(), request.getData());

        NotificationLog logEntry = createLogEntry(request, "PENDING", null);
        logEntry.setRenderedContent("Subject: " + subject);
        notificationLogServiceAuditable.create(logEntry);

        try {
            if (canSes) {
                sendViaSes(request, recipientEmail, subject, body, sesClient, logEntry);
            } else if (canSendgrid) {
                sendViaSendGrid(request, recipientEmail, subject, body, sendGrid, logEntry);
            } else {
                throw new IllegalStateException("No usable email backend (SES client/from-email or SendGrid/from-email)");
            }
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=EMAIL eventId={} templateKey={} recipientEmail={} error={}",
                    request.getEventId(), request.getTemplateKey(), recipientEmail, e.getMessage(), e);
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
            notificationLogServiceAuditable.update(logEntry);
        }
    }

    private void sendViaSes(
            NotificationRequest request,
            String recipientEmail,
            String subject,
            String body,
            SesClient sesClient,
            NotificationLog logEntry) {

        software.amazon.awssdk.services.ses.model.Content subjectContent =
                software.amazon.awssdk.services.ses.model.Content.builder().data(subject).build();
        software.amazon.awssdk.services.ses.model.Content bodyContent =
                software.amazon.awssdk.services.ses.model.Content.builder().data(body).build();
        Body emailBody = Body.builder().html(bodyContent).build();
        Message message = Message.builder().subject(subjectContent).body(emailBody).build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(recipientEmail).build())
                .message(message)
                .source(sesFromEmail)
                .build();

        log.info("[NOTIFICATION] Attempting send channel=EMAIL provider=AWS_SES eventId={} templateKey={} recipientEmail={}",
                request.getEventId(), request.getTemplateKey(), recipientEmail);
        SendEmailResponse response = sesClient.sendEmail(emailRequest);

        logEntry.setStatus("SENT");
        logEntry.setProvider("AWS_SES");
        logEntry.setProviderMessageId(response.messageId());
        notificationLogServiceAuditable.update(logEntry);
        log.info("[NOTIFICATION] Sent channel=EMAIL provider=AWS_SES eventId={} templateKey={} recipientEmail={} messageId={}",
                request.getEventId(), request.getTemplateKey(), recipientEmail, response.messageId());
    }

    private void sendViaSendGrid(
            NotificationRequest request,
            String recipientEmail,
            String subject,
            String body,
            SendGrid sendGrid,
            NotificationLog logEntry) throws IOException {

        Email from = new Email(sendgridFromEmail);
        Email to = new Email(recipientEmail);
        Content htmlContent = new Content("text/html", body);
        Mail mail = new Mail(from, subject, to, htmlContent);

        Request sgRequest = new Request();
        sgRequest.setMethod(Method.POST);
        sgRequest.setEndpoint("mail/send");
        sgRequest.setBody(mail.build());

        log.info("[NOTIFICATION] Attempting send channel=EMAIL provider=SENDGRID eventId={} templateKey={} recipientEmail={}",
                request.getEventId(), request.getTemplateKey(), recipientEmail);
        Response response = sendGrid.api(sgRequest);
        int code = response.getStatusCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("SendGrid HTTP " + code + ": " + response.getBody());
        }

        String messageId = extractSendGridMessageId(response);
        logEntry.setStatus("SENT");
        logEntry.setProvider("SENDGRID");
        logEntry.setProviderMessageId(messageId != null ? messageId : ("sendgrid-http-" + code));
        notificationLogServiceAuditable.update(logEntry);
        log.info("[NOTIFICATION] Sent channel=EMAIL provider=SENDGRID eventId={} templateKey={} recipientEmail={} messageId={}",
                request.getEventId(), request.getTemplateKey(), recipientEmail, messageId);
    }

    private static String extractSendGridMessageId(Response response) {
        Object headers = response.getHeaders();
        if (!(headers instanceof Map<?, ?> map)) {
            return null;
        }
        Object id = map.get("X-Message-Id");
        if (id == null) {
            id = map.get("x-message-id");
        }
        return id != null ? id.toString() : null;
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
