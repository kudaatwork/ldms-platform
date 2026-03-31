package projectlx.co.zw.notificationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.notificationsmanagementservice.model.NotificationLog;
import projectlx.co.zw.notificationsmanagementservice.model.NotificationTemplate;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository <NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

}
