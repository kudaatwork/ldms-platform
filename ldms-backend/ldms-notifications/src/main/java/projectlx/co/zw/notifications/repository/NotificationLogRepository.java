package projectlx.co.zw.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.notifications.model.NotificationLog;

public interface NotificationLogRepository extends JpaRepository <NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

}
