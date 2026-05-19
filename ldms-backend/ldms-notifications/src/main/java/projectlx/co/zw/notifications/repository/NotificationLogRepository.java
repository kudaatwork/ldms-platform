package projectlx.co.zw.notifications.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

    Optional<NotificationLog> findFirstByEventIdAndChannelAndEntityStatusNot(
            String eventId, Channel channel, EntityStatus entityStatus);

    List<NotificationLog> findByEventIdAndEntityStatusNot(String eventId, EntityStatus entityStatus);
}
