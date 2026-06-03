package projectlx.co.zw.notifications.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

    Optional<NotificationLog> findFirstByEventIdAndChannelAndEntityStatusNot(
            String eventId, Channel channel, EntityStatus entityStatus);

    List<NotificationLog> findByEventIdAndEntityStatusNot(String eventId, EntityStatus entityStatus);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.entityStatus = :deletedStatus, n.updatedAt = :updatedAt "
            + "WHERE n.entityStatus IS NULL OR n.entityStatus <> :deletedStatus")
    int softDeleteAll(@Param("deletedStatus") EntityStatus deletedStatus, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.entityStatus = :deletedStatus, n.updatedAt = :updatedAt "
            + "WHERE (n.entityStatus IS NULL OR n.entityStatus <> :deletedStatus) AND n.createdAt < :cutoff")
    int softDeleteBefore(@Param("cutoff") LocalDateTime cutoff,
                         @Param("deletedStatus") EntityStatus deletedStatus,
                         @Param("updatedAt") LocalDateTime updatedAt);
}
