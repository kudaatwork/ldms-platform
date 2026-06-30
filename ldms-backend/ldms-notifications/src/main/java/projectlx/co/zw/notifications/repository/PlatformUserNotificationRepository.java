package projectlx.co.zw.notifications.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.co.zw.notifications.model.PlatformUserNotification;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface PlatformUserNotificationRepository extends JpaRepository<PlatformUserNotification, Long> {

    Optional<PlatformUserNotification> findByIdAndUserIdAndEntityStatusNot(Long id, Long userId, EntityStatus excluded);

    List<PlatformUserNotification> findByUserIdAndDismissedAtIsNullAndEntityStatusNotOrderByCreatedAtDesc(
            Long userId, EntityStatus excluded, Pageable pageable);

    List<PlatformUserNotification> findByUserIdAndDismissedAtIsNullAndEntityStatusNotAndIdGreaterThanOrderByCreatedAtDesc(
            Long userId, EntityStatus excluded, Long afterId, Pageable pageable);

    Optional<PlatformUserNotification> findByUserIdAndSourceEventIdAndEntityStatusNot(
            Long userId, String sourceEventId, EntityStatus excluded);

    List<PlatformUserNotification> findByUserIdAndDismissedAtIsNullAndEntityStatusNot(
            Long userId, EntityStatus excluded);
}
