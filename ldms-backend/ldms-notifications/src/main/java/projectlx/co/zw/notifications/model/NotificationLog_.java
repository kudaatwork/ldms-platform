package projectlx.co.zw.notifications.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import java.util.Map;
import javax.annotation.processing.Generated;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(NotificationLog.class)
public class NotificationLog_ {
    public static volatile SingularAttribute<NotificationLog, Long> id;
    public static volatile SingularAttribute<NotificationLog, String> eventId;
    public static volatile SingularAttribute<NotificationLog, String> recipientId;
    public static volatile SingularAttribute<NotificationLog, String> recipientEmail;
    public static volatile SingularAttribute<NotificationLog, String> recipientPhone;
    public static volatile SingularAttribute<NotificationLog, String> templateKey;
    public static volatile SingularAttribute<NotificationLog, Channel> channel;
    public static volatile SingularAttribute<NotificationLog, String> status;
    public static volatile SingularAttribute<NotificationLog, String> provider;
    public static volatile SingularAttribute<NotificationLog, String> providerMessageId;
    public static volatile SingularAttribute<NotificationLog, Map<String, Object>> payload;
    public static volatile SingularAttribute<NotificationLog, String> renderedContent;
    public static volatile SingularAttribute<NotificationLog, String> errorMessage;
    public static volatile SingularAttribute<NotificationLog, LocalDateTime> createdAt;
    public static volatile SingularAttribute<NotificationLog, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<NotificationLog, EntityStatus> entityStatus;
}
