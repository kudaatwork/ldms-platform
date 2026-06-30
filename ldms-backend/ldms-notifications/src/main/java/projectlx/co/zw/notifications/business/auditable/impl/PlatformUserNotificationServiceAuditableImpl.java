package projectlx.co.zw.notifications.business.auditable.impl;

import projectlx.co.zw.notifications.business.auditable.api.PlatformUserNotificationServiceAuditable;
import projectlx.co.zw.notifications.model.PlatformUserNotification;
import projectlx.co.zw.notifications.repository.PlatformUserNotificationRepository;

import java.util.Locale;

public class PlatformUserNotificationServiceAuditableImpl implements PlatformUserNotificationServiceAuditable {

    private final PlatformUserNotificationRepository repository;

    public PlatformUserNotificationServiceAuditableImpl(PlatformUserNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PlatformUserNotification create(PlatformUserNotification notification, Locale locale, String username) {
        notification.setCreatedBy(username);
        notification.setModifiedBy(username);
        return repository.save(notification);
    }

    @Override
    public PlatformUserNotification update(PlatformUserNotification notification, Locale locale, String username) {
        notification.setModifiedBy(username);
        return repository.save(notification);
    }
}
