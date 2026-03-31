package projectlx.co.zw.notificationsmanagementservice.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.notificationsmanagementservice.model.NotificationTemplate;
import projectlx.co.zw.notificationsmanagementservice.model.NotificationTemplate_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class NotificationTemplateSpecification {

    public static Specification<NotificationTemplate> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(NotificationTemplate_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<NotificationTemplate> templateKeyLike(final String templateKey) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(NotificationTemplate_.templateKey).as(String.class), templateKey + "%");
            return p;
        };
    }

    public static Specification<NotificationTemplate> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(NotificationTemplate_.description).as(String.class), "%" + description + "%");
            return p;
        };
    }

    public static Specification<NotificationTemplate> inAppTitleLike(final String inAppTitle) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(NotificationTemplate_.inAppTitle).as(String.class), "%" + inAppTitle + "%");
            return p;
        };
    }

    public static Specification<NotificationTemplate> whatsappTemplateNameLike(final String whatsappTemplateName) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(NotificationTemplate_.whatsappTemplateName).as(String.class), "%" + whatsappTemplateName + "%");
            return p;
        };
    }

    public static Specification<NotificationTemplate> hasChannel(final String channel) {
        return (root, query, cb) -> {
            // This is a simplification - in a real implementation, you would need to check if the channel is in the JSON array
            Predicate p = cb.like(root.get(NotificationTemplate_.channels).as(String.class), "%" + channel + "%");
            return p;
        };
    }

    public static Specification<NotificationTemplate> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(NotificationTemplate_.templateKey), "%" + search + "%"),
                    cb.like(root.get(NotificationTemplate_.description), "%" + search + "%"),
                    cb.like(root.get(NotificationTemplate_.emailSubject), "%" + search + "%"),
                    cb.like(root.get(NotificationTemplate_.inAppTitle), "%" + search + "%"),
                    cb.like(root.get(NotificationTemplate_.whatsappTemplateName), "%" + search + "%")
            );
            return p;
        };
    }

    public static Specification<NotificationTemplate> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(NotificationTemplate_.entityStatus), entityStatus);
            return p;
        };
    }

    public static Specification<NotificationTemplate> isActive(final boolean isActive) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(NotificationTemplate_.isActive), isActive);
            return p;
        };
    }
}
