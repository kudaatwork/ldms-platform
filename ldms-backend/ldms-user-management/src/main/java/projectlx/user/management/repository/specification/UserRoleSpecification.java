package projectlx.user.management.repository.specification;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserRole;
import projectlx.user.management.model.UserRole_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserRoleSpecification {

    public static Specification<UserRole> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(UserRole_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<UserRole> roleLike(final String role) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserRole_.role).as(String.class), role + "%");
            return p;
        };
    }

    public static Specification<UserRole> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserRole_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<UserRole> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(UserRole_.role), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserRole_.description), "%" + search.toUpperCase() + "%")
            );

            return p;
        };
    }
}
