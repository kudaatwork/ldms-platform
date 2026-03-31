package projectlx.co.zw.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.UserNotificationPreference;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.util.Optional;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long>, JpaSpecificationExecutor<UserNotificationPreference> {
    Optional<UserNotificationPreference> findByUserIdAndTemplateKeyAndChannelAndEntityStatusNot(String userId, String templateKey,
                                                                                                Channel channel, EntityStatus entityStatus);
}
