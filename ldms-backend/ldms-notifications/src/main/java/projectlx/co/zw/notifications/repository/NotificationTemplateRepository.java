package projectlx.co.zw.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long>, JpaSpecificationExecutor<NotificationTemplate> {
    Optional<NotificationTemplate> findByTemplateKeyAndEntityStatusNot(String templateKey, EntityStatus entityStatus);
}
