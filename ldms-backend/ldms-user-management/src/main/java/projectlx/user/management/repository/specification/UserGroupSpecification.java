package projectlx.user.management.repository.specification;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.model.UserGroup_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserGroupSpecification {

    public static Specification<UserGroup> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(UserGroup_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<UserGroup> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserGroup_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<UserGroup> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserGroup_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<UserGroup> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(UserGroup_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserGroup_.description), "%" + search.toUpperCase() + "%")
            );

            return p;
        };
    }
}
