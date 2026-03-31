package projectlx.co.zw.notifications.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.notifications.business.logic.api.NotificationTemplateService;
import projectlx.co.zw.notifications.service.processor.api.NotificationTemplateProcessor;
import projectlx.co.zw.notifications.service.processor.impl.NotificationTemplateProcessorImpl;

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
}