package projectlx.co.zw.notifications.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogService;
import projectlx.co.zw.notifications.business.logic.api.NotificationTemplateService;
import projectlx.co.zw.notifications.business.logic.api.PlatformUserNotificationService;
import projectlx.co.zw.notifications.service.processor.api.NotificationLogProcessor;
import projectlx.co.zw.notifications.service.processor.api.NotificationTemplateProcessor;
import projectlx.co.zw.notifications.service.processor.api.PlatformUserNotificationProcessor;
import projectlx.co.zw.notifications.service.processor.impl.NotificationLogProcessorImpl;
import projectlx.co.zw.notifications.service.processor.impl.NotificationTemplateProcessorImpl;
import projectlx.co.zw.notifications.service.processor.impl.PlatformUserNotificationProcessorImpl;

/**
 * Configuration class for service processors
 */
@Configuration
public class ServiceConfig {

    /**
     * Creates a bean for NotificationTemplateProcessor
     *
     * @param notificationTemplateService the notification template service
     * @return the notification template processor
     */
    @Bean
    public NotificationTemplateProcessor notificationTemplateProcessor(NotificationTemplateService notificationTemplateService) {
        return new NotificationTemplateProcessorImpl(notificationTemplateService);
    }

    @Bean
    public NotificationLogProcessor notificationLogProcessor(NotificationLogService notificationLogService) {
        return new NotificationLogProcessorImpl(notificationLogService);
    }

    @Bean
    public PlatformUserNotificationProcessor platformUserNotificationProcessor(
            PlatformUserNotificationService platformUserNotificationService,
            JwtService jwtService) {
        return new PlatformUserNotificationProcessorImpl(platformUserNotificationService, jwtService);
    }
}