package projectlx.co.zw.notifications.business.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.firebase.messaging.FirebaseMessaging;
import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.business.auditable.impl.NotificationLogServiceAuditableImpl;
import projectlx.co.zw.notifications.business.logic.api.NotificationService;
import projectlx.co.zw.notifications.business.logic.api.NotificationTemplateService;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.business.logic.impl.EmailNotificationProviderServiceImpl;
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
import projectlx.co.zw.notifications.business.validation.api.NotificationTemplateServiceValidator;
import projectlx.co.zw.notifications.business.validation.api.TemplateProcessorServiceValidator;
import projectlx.co.zw.notifications.business.validation.impl.AuditTrailServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.EmailNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.InAppNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.NotificationServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.NotificationTemplateServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.SmsNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.TemplateProcessorServiceValidatorImpl;
import projectlx.co.zw.notifications.business.validation.impl.WhatsAppNotificationProviderServiceValidatorImpl;
import projectlx.co.zw.notifications.repository.NotificationLogRepository;
import projectlx.co.zw.notifications.repository.NotificationTemplateRepository;
import projectlx.co.zw.notifications.repository.UserNotificationPreferenceRepository;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.ses.SesClient;
import java.util.List;

@Configuration
public class BusinessConfig {

    @Bean
    public NotificationLogServiceAuditable notificationLogServiceAuditable(NotificationLogRepository notificationLogRepository) {
        return new NotificationLogServiceAuditableImpl(notificationLogRepository);
    }

    @Bean
    public NotificationService notificationService(NotificationTemplateRepository templateRepository,
                                                   UserNotificationPreferenceRepository preferenceRepository,
                                                   List<NotificationProviderService> providers,
                                                   NotificationServiceValidator validator,
                                                   NotificationLogServiceAuditable notificationLogServiceAuditable) {
        return new NotificationServiceImpl(templateRepository, preferenceRepository, providers, validator,
                notificationLogServiceAuditable);
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
            ModelMapper modelMapper) {
        return new NotificationTemplateServiceImpl(
                notificationTemplateServiceValidator,
                notificationTemplateRepository,
                messageService,
                modelMapper);
    }

    @Bean
    public TemplateProcessorService templateProcessorService(
            TemplateProcessorServiceValidator templateProcessorServiceValidator) {
        return new TemplateProcessorServiceImpl(templateProcessorServiceValidator);
    }

    @Bean
    public NotificationProviderService emailNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogServiceAuditable notificationLogServiceAuditable,
            SesClient sesClient) {
        return new EmailNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogServiceAuditable,
                sesClient);
    }

    @Bean
    public NotificationProviderService inAppNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogServiceAuditable notificationLogServiceAuditable,
            FirebaseMessaging firebaseMessaging) {
        return new InAppNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogServiceAuditable,
                firebaseMessaging);
    }

    @Bean
    public NotificationProviderService smsNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogServiceAuditable notificationLogServiceAuditable) {
        return new SmsNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogServiceAuditable);
    }

    @Bean
    public NotificationProviderService whatsAppNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogServiceAuditable notificationLogServiceAuditable) {
        return new WhatsAppNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogServiceAuditable);
    }

    @Bean
    public RestTemplate notificationsRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public NotificationProviderService slackNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogServiceAuditable notificationLogServiceAuditable,
            RestTemplate notificationsRestTemplate) {
        return new SlackNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogServiceAuditable,
                notificationsRestTemplate);
    }

    @Bean
    public NotificationProviderService teamsNotificationProviderService(
            TemplateProcessorService templateProcessorService,
            NotificationLogServiceAuditable notificationLogServiceAuditable,
            RestTemplate notificationsRestTemplate) {
        return new TeamsNotificationProviderServiceImpl(
                templateProcessorService,
                notificationLogServiceAuditable,
                notificationsRestTemplate);
    }
}
