package projectlx.co.zw.notifications.business.auditable.impl;

import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.repository.NotificationLogRepository;

public class NotificationLogServiceAuditableImpl implements NotificationLogServiceAuditable {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationLogServiceAuditableImpl(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public NotificationLog create(NotificationLog notificationLog) {
        return notificationLogRepository.save(notificationLog);
    }

    @Override
    public NotificationLog update(NotificationLog notificationLog) {
        return notificationLogRepository.save(notificationLog);
    }

    @Override
    public NotificationLog delete(NotificationLog notificationLog) {
        return notificationLogRepository.save(notificationLog);
    }
}
