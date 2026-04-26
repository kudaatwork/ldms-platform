package projectlx.user.management.repository.specification;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserPreferences;
import projectlx.user.management.model.UserPreferences_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserPreferencesSpecification {

    public static Specification<UserPreferences> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(UserPreferences_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<UserPreferences> preferredLanguageLike(final String preferredLanguage) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserPreferences_.preferredLanguage).as(String.class), preferredLanguage + "%");
            return p;
        };
    }

    public static Specification<UserPreferences> timezoneLike(final String timezone) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserPreferences_.timezone).as(String.class), timezone + "%");
            return p;
        };
    }

    public static Specification<UserPreferences> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(UserPreferences_.preferredLanguage), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserPreferences_.timezone), "%" + search.toUpperCase() + "%")
            );

            return p;
        };
    }
}
