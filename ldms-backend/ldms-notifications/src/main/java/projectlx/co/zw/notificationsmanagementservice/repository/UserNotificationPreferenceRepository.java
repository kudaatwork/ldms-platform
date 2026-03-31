package projectlx.co.zw.notificationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.notificationsmanagementservice.model.Channel;
import projectlx.co.zw.notificationsmanagementservice.model.UserNotificationPreference;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.util.Optional;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long>, JpaSpecificationExecutor<UserNotificationPreference> {
    Optional<UserNotificationPreference> findByUserIdAndTemplateKeyAndChannelAndEntityStatusNot(String userId, String templateKey,
                                                                                                Channel channel, EntityStatus entityStatus);
}
