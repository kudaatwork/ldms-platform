package projectlx.co.zw.notifications.repository.specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationLog_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public final class NotificationLogSpecification {

    private NotificationLogSpecification() {
    }

    public static Specification<NotificationLog> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get(NotificationLog_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<NotificationLog> templateKeyLike(final String templateKey) {
        return (root, query, cb) ->
                cb.like(root.get(NotificationLog_.templateKey), "%" + templateKey.trim() + "%");
    }

    public static Specification<NotificationLog> statusEquals(final String status) {
        return (root, query, cb) -> cb.equal(root.get(NotificationLog_.status), status.trim());
    }

    public static Specification<NotificationLog> recipientIdEquals(final String recipientId) {
        return (root, query, cb) -> cb.equal(root.get(NotificationLog_.recipientId), recipientId.trim());
    }

    public static Specification<NotificationLog> channelEquals(final String channel) {
        return (root, query, cb) ->
                cb.equal(root.get(NotificationLog_.channel), projectlx.co.zw.notifications.model.Channel.valueOf(channel.trim()));
    }

    public static Specification<NotificationLog> providerLike(final String provider) {
        final String like = "%" + provider.trim().toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(NotificationLog_.provider)), like);
    }

    public static Specification<NotificationLog> createdBetween(final LocalDateTime from, final LocalDateTime to) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();
            if (from != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get(NotificationLog_.createdAt), from));
            }
            if (to != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get(NotificationLog_.createdAt), to));
            }
            return p;
        };
    }

    public static Specification<NotificationLog> any(final String search) {
        final String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get(NotificationLog_.templateKey)), like),
                cb.like(cb.lower(root.get(NotificationLog_.recipientId)), like),
                cb.like(cb.lower(root.get(NotificationLog_.recipientEmail)), like),
                cb.like(cb.lower(root.get(NotificationLog_.recipientPhone)), like),
                cb.like(cb.lower(root.get(NotificationLog_.status)), like),
                cb.like(cb.lower(root.get(NotificationLog_.eventId)), like));
    }
}
