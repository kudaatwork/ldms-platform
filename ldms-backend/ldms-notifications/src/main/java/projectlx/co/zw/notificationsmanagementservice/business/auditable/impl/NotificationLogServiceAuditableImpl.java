package projectlx.co.zw.notificationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.notificationsmanagementservice.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notificationsmanagementservice.model.NotificationLog;
import projectlx.co.zw.notificationsmanagementservice.repository.NotificationLogRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class NotificationLogServiceAuditableImpl implements NotificationLogServiceAuditable {

    private final NotificationLogRepository notificationLogRepository;

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
