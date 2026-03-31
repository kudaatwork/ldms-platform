package projectlx.co.zw.notifications.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import java.util.List;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(NotificationTemplate.class)
public class NotificationTemplate_ {
    public static volatile SingularAttribute<NotificationTemplate, Long> id;
    public static volatile SingularAttribute<NotificationTemplate, String> templateKey;
    public static volatile SingularAttribute<NotificationTemplate, String> description;
    public static volatile SingularAttribute<NotificationTemplate, List<Channel>> channels;
    public static volatile SingularAttribute<NotificationTemplate, String> emailSubject;
    public static volatile SingularAttribute<NotificationTemplate, String> emailBodyHtml;
    public static volatile SingularAttribute<NotificationTemplate, String> smsBody;
    public static volatile SingularAttribute<NotificationTemplate, String> inAppTitle;
    public static volatile SingularAttribute<NotificationTemplate, String> inAppBody;
    public static volatile SingularAttribute<NotificationTemplate, String> whatsappTemplateName;
    public static volatile SingularAttribute<NotificationTemplate, Boolean> isActive;
    public static volatile SingularAttribute<NotificationTemplate, LocalDateTime> createdAt;
    public static volatile SingularAttribute<NotificationTemplate, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<NotificationTemplate, EntityStatus> entityStatus;
}