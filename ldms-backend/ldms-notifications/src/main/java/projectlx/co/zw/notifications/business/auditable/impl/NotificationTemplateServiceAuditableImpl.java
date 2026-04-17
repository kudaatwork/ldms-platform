package projectlx.co.zw.notifications.business.auditable.impl;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import projectlx.co.zw.notifications.business.auditable.api.NotificationTemplateServiceAuditable;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.repository.NotificationTemplateRepository;

@RequiredArgsConstructor
public class NotificationTemplateServiceAuditableImpl implements NotificationTemplateServiceAuditable {

    private final NotificationTemplateRepository notificationTemplateRepository;

    @Override
    public NotificationTemplate create(NotificationTemplate template, Locale locale, String username) {
        return notificationTemplateRepository.save(template);
    }

    @Override
    public NotificationTemplate update(NotificationTemplate template, Locale locale, String username) {
        return notificationTemplateRepository.save(template);
    }

    @Override
    public NotificationTemplate delete(NotificationTemplate template, Locale locale, String username) {
        return notificationTemplateRepository.save(template);
    }
}
