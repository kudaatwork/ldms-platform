package projectlx.co.zw.notifications.business.logic.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.utils.config.OutboundEmailClientSupplier;
import projectlx.co.zw.notifications.utils.config.OutboundSesClientSupplier;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.config.OutboundMessagingReadiness;
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
    private final NotificationLogRecorder notificationLogRecorder;
    private final OutboundSesClientSupplier outboundSesClientSupplier;
    private final OutboundEmailClientSupplier outboundEmailClientSupplier;
    private final OutboundMessagingReadiness outboundMessagingReadiness;

    @Value("${notifications.email.provider:ses}")
    private String emailProviderPreference;

    public EmailNotificationProviderServiceImpl(
            TemplateProcessorService templateProcessor,
            NotificationLogRecorder notificationLogRecorder,
            OutboundSesClientSupplier outboundSesClientSupplier,
            OutboundEmailClientSupplier outboundEmailClientSupplier,
            OutboundMessagingReadiness outboundMessagingReadiness) {
        this.templateProcessor = templateProcessor;
        this.notificationLogRecorder = notificationLogRecorder;
        this.outboundSesClientSupplier = outboundSesClientSupplier;
        this.outboundEmailClientSupplier = outboundEmailClientSupplier;
        this.outboundMessagingReadiness = outboundMessagingReadiness;
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
            notificationLogRecorder.markSkipped(request, Channel.EMAIL, "missing_recipient email");
            return;
        }

        SesClient sesClient = outboundSesClientSupplier.getIfAvailable();
        com.sendgrid.SendGrid sendGrid = outboundEmailClientSupplier.getIfAvailable();
        boolean preferSes = prefersSes();
        boolean canSes = sesClient != null && outboundMessagingReadiness.isSesEmailReady();
        boolean canSendgrid = sendGrid != null && outboundMessagingReadiness.isSendGridEmailReady();
        if (preferSes && !canSes) {
            log.warn("[NOTIFICATION] Skipped channel=EMAIL eventId={} templateKey={} reason=ses_not_configured "
                            + "(set AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SES_FROM_EMAIL, AWS_REGION in ldms-config-repo/.env)",
                    request.getEventId(), request.getTemplateKey());
            notificationLogRecorder.markSkipped(request, Channel.EMAIL,
                    "ses_not_configured (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SES_FROM_EMAIL, AWS_REGION)");
            return;
        }
        if (!canSes && !canSendgrid) {
            log.warn("[NOTIFICATION] Skipped channel=EMAIL eventId={} templateKey={} reason=no_email_backend",
                    request.getEventId(), request.getTemplateKey());
            notificationLogRecorder.markSkipped(request, Channel.EMAIL,
                    preferSes
                            ? "ses_not_configured (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SES_FROM_EMAIL, AWS_REGION)"
                            : "no_email_backend (configure AWS SES or SendGrid)");
            return;
        }

        String subject = templateProcessor.process(template.getEmailSubject(), request.getData());
        String body = templateProcessor.process(template.getEmailBodyHtml(), request.getData());

        NotificationLog logEntry = notificationLogRecorder.beginDispatch(request, Channel.EMAIL);
        logEntry.setRenderedContent("Subject: " + subject);

        try {
            if (preferSes && canSes) {
                sendViaSes(request, recipientEmail, subject, body, sesClient, logEntry);
            } else if (canSendgrid) {
                sendViaSendGrid(request, recipientEmail, subject, body, sendGrid, logEntry);
            } else {
                sendViaSes(request, recipientEmail, subject, body, sesClient, logEntry);
            }
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed channel=EMAIL eventId={} templateKey={} recipientEmail={} error={}",
                    request.getEventId(), request.getTemplateKey(), recipientEmail, e.getMessage(), e);
            notificationLogRecorder.markFailed(logEntry, e.getMessage());
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
                .source(outboundSesClientSupplier.fromEmail())
                .build();

        log.info("[NOTIFICATION] Attempting send channel=EMAIL provider=AWS_SES eventId={} templateKey={} recipientEmail={}",
                request.getEventId(), request.getTemplateKey(), recipientEmail);
        SendEmailResponse response = sesClient.sendEmail(emailRequest);

        notificationLogRecorder.markSent(logEntry, "AWS_SES", response.messageId(), logEntry.getRenderedContent());
        log.info("[NOTIFICATION] Sent channel=EMAIL provider=AWS_SES eventId={} templateKey={} recipientEmail={} messageId={}",
                request.getEventId(), request.getTemplateKey(), recipientEmail, response.messageId());
    }

    private void sendViaSendGrid(
            NotificationRequest request,
            String recipientEmail,
            String subject,
            String body,
            com.sendgrid.SendGrid sendGrid,
            NotificationLog logEntry) throws IOException {

        Email from = new Email(outboundEmailClientSupplier.fromEmail());
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
        notificationLogRecorder.markSent(logEntry, "SENDGRID",
                messageId != null ? messageId : ("sendgrid-http-" + code), logEntry.getRenderedContent());
        log.info("[NOTIFICATION] Sent channel=EMAIL provider=SENDGRID eventId={} templateKey={} recipientEmail={} messageId={}",
                request.getEventId(), request.getTemplateKey(), recipientEmail, messageId);
    }

    private boolean prefersSes() {
        if (!StringUtils.hasText(emailProviderPreference)) {
            return true;
        }
        return !"sendgrid".equalsIgnoreCase(emailProviderPreference.trim());
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
}
