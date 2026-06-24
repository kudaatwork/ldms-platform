package projectlx.co.zw.notifications.business.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.firebase.messaging.FirebaseMessaging;
import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.business.auditable.api.NotificationTemplateServiceAuditable;
import projectlx.co.zw.notifications.business.auditable.impl.NotificationLogServiceAuditableImpl;
import projectlx.co.zw.notifications.business.auditable.impl.NotificationTemplateServiceAuditableImpl;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogService;
import projectlx.co.zw.notifications.business.logic.api.NotificationService;
import projectlx.co.zw.notifications.business.logic.api.NotificationTemplateService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.business.logic.impl.EmailNotificationProviderServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.NotificationLogRecorderImpl;
import projectlx.co.zw.notifications.business.logic.impl.NotificationLogServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.TemplateProcessorServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.InAppNotificationProviderServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.NotificationServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.NotificationTemplateServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.SlackNotificationProviderServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.SmsNotificationProviderServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.TeamsNotificationProviderServiceImpl;
import projectlx.co.zw.notifications.business.logic.impl.WhatsAppNotificationProviderServiceImpl;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.validation.api.AuditTrailServiceValidator;
import projectlx.co.zw.notifications.business.validation.api.NotificationProviderServiceValidator;
import projectlx.co.zw.notifications.business.validation.api.NotificationServiceValidator;
import projectlx.co.zw.notifications.business.validation.api.NotificationLogServiceValidator;
import projectlx.co.zw.notifications.business.validation.api.NotificationTemplateServiceValidator;
import projectlx.co.zw.notifications.business.validation.impl.NotificationLogServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.api.TemplateProcessorServiceValidator;
import projectlx.co.zw.notifications.business.validation.impl.AuditTrailServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.EmailNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.InAppNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.NotificationServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.NotificationTemplateServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.SmsNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.TemplateProcessorServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.WhatsAppNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.utils.config.OutboundMessagingReadiness;
import projectlx.co.zw.notifications.business.logic.support.NotificationBillingSupport;
import projectlx.co.zw.notifications.clients.BillingPaymentsServiceClient;
import projectlx.co.zw.notifications.repository.NotificationLogRepository;
import projectlx.co.zw.notifications.repository.NotificationTemplateRepository;
import projectlx.co.zw.notifications.repository.UserNotificationPreferenceRepository;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import org.springframework.web.client.RestTemplate;
import projectlx.co.zw.notifications.utils.config.LdmsConfigRepoSecretsResolver;
import projectlx.co.zw.notifications.utils.config.OutboundEmailClientSupplier;
import projectlx.co.zw.notifications.utils.config.OutboundSesClientSupplier;
import projectlx.co.zw.notifications.utils.config.OutboundTwilioInitializer;
import java.util.List;

@Configuration
public class BusinessConfig {

    @Bean
    public NotificationLogServiceAuditable notificationLogServiceAuditable(NotificationLogRepository notificationLogRepository) {
        return new NotificationLogServiceAuditableImpl(notificationLogRepository);
    }

    @Bean
    public NotificationTemplateServiceAuditable notificationTemplateServiceAuditable(
            NotificationTemplateRepository notificationTemplateRepository) {
        return new NotificationTemplateServiceAuditableImpl(notificationTemplateRepository);
    }

    @Bean
    public NotificationBillingSupport notificationBillingSupport(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        return new NotificationBillingSupport(billingPaymentsServiceClient);
    }

    @Bean
    public NotificationLogRecorder notificationLogRecorder(
            NotificationLogRepository notificationLogRepository,
            NotificationLogServiceAuditable notificationLogServiceAuditable) {
        return new NotificationLogRecorderImpl(notificationLogRepository, notificationLogServiceAuditable);
    }

    @Bean
    public NotificationLogServiceValidator notificationLogServiceValidator() {
        return new NotificationLogServiceValidatorImpl();
    }

    @Bean
    public NotificationLogService notificationLogService(
            NotificationLogRepository notificationLogRepository,
            NotificationLogServiceValidator notificationLogServiceValidator,
            RabbitAdmin rabbitAdmin) {
        return new NotificationLogServiceImpl(notificationLogRepository, notificationLogServiceValidator, rabbitAdmin);
    }

    @Bean
    public NotificationService notificationService(NotificationTemplateRepository templateRepository,
                                                   UserNotificationPreferenceRepository preferenceRepository,
                                                   List<NotificationProviderService> providers,
                                                   NotificationServiceValidator validator,
                                                   NotificationLogRecorder notificationLogRecorder) {
        return new NotificationServiceImpl(templateRepository, preferenceRepository, providers, validator,
                notificationLogRecorder);
    }

    // Validator beans

    @Bean
    public NotificationServiceValidator notificationServiceValidator(MessageService messageService) {
        return new NotificationServiceValidatorImpl(messageService);
    }

    @Bean
    public AuditTrailServiceValidator auditTrailServiceValidator(MessageService messageService) {
        return new AuditTrailServiceValidatorImpl(messageService);
    }

    @Bean
    public TemplateProcessorServiceValidator templateProcessorServiceValidator(MessageService messageService) {
        return new TemplateProcessorServiceValidatorImpl(messageService);
    }

    @Bean
    public NotificationProviderServiceValidator emailNotificationProviderServiceValidator(MessageService messageService) {
        return new EmailNotificationProviderServiceValidatorImpl(messageService);
    }

    @Bean
    public NotificationProviderServiceValidator smsNotificationProviderServiceValidator(MessageService messageService) {
        return new SmsNotificationProviderServiceValidatorImpl(messageService);
    }

    @Bean
    public NotificationProviderServiceValidator whatsAppNotificationProviderServiceValidator(MessageService messageService) {
        return new WhatsAppNotificationProviderServiceValidatorImpl(messageService);
    }

    @Bean
    public NotificationProviderServiceValidator inAppNotificationProviderServiceValidator(MessageService messageService) {
        return new InAppNotificationProviderServiceValidatorImpl(messageService);
    }

    @Bean
    public NotificationTemplateServiceValidator notificationTemplateServiceValidator(MessageService messageService) {
        return new NotificationTemplateServiceValidatorImpl(messageService);
    }

    @Bean
    public NotificationTemplateService notificationTemplateService(
            NotificationTemplateServiceValidator notificationTemplateServiceValidator,
            NotificationTemplateRepository notificationTemplateRepository,
            MessageService messageService,
            ModelMapper modelMapper,
            NotificationTemplateServiceAuditable notificationTemplateServiceAuditable) {
        return new NotificationTemplateServiceImpl(
                notificationTemplateServiceValidator,
                notificationTemplateRepository,
                messageService,
                modelMapper,
                notificationTemplateServiceAuditable);
    }

    @Bean
    public TemplateProcessorService templateProcessorService(
            TemplateProcessorServiceValidator templateProcessorServiceValidator) {
        return new TemplateProcessorServiceImpl(templateProcessorServiceValidator);
    }

    @Bean
    public NotificationProviderService emailNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogRecorder notificationLogRecorder,
            OutboundSesClientSupplier outboundSesClientSupplier,
            OutboundEmailClientSupplier outboundEmailClientSupplier,
            OutboundMessagingReadiness outboundMessagingReadiness) {
        return new EmailNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogRecorder,
                outboundSesClientSupplier,
                outboundEmailClientSupplier,
                outboundMessagingReadiness);
    }

    @Bean
    public NotificationProviderService inAppNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogRecorder notificationLogRecorder,
            FirebaseMessaging firebaseMessaging) {
        return new InAppNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogRecorder,
                firebaseMessaging);
    }

    @Bean
    public NotificationProviderService smsNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogRecorder notificationLogRecorder,
            OutboundMessagingReadiness outboundMessagingReadiness,
            OutboundTwilioInitializer outboundTwilioInitializer,
            LdmsConfigRepoSecretsResolver secretsResolver,
            NotificationBillingSupport notificationBillingSupport) {
        return new SmsNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogRecorder,
                outboundMessagingReadiness,
                outboundTwilioInitializer,
                secretsResolver,
                notificationBillingSupport);
    }

    @Bean
    public NotificationProviderService whatsAppNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogRecorder notificationLogRecorder,
            OutboundMessagingReadiness outboundMessagingReadiness,
            OutboundTwilioInitializer outboundTwilioInitializer,
            LdmsConfigRepoSecretsResolver secretsResolver,
            NotificationBillingSupport notificationBillingSupport) {
        return new WhatsAppNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogRecorder,
                outboundMessagingReadiness,
                outboundTwilioInitializer,
                secretsResolver,
                notificationBillingSupport);
    }

    @Bean
    public RestTemplate notificationsRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public NotificationProviderService slackNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogRecorder notificationLogRecorder,
            @Qualifier("notificationsRestTemplate") RestTemplate notificationsRestTemplate) {
        return new SlackNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogRecorder,
                notificationsRestTemplate);
    }

    @Bean
    public NotificationProviderService teamsNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogRecorder notificationLogRecorder,
            @Qualifier("notificationsRestTemplate") RestTemplate notificationsRestTemplate) {
        return new TeamsNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogRecorder,
                notificationsRestTemplate);
    }
}
