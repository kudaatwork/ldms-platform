package projectlx.user.management.service.repository.specification;

import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.UserType;
import projectlx.user.management.service.model.UserType_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserTypeSpecification {

    public static Specification<UserType> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(UserType_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<UserType> userTypeNameLike(final String type) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserType_.userTypeName).as(String.class), type + "%");
            return p;
        };
    }

    public static Specification<UserType> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserType_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<UserType> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(UserType_.userTypeName), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserType_.description), "%" + search.toUpperCase() + "%")
            );

            return p;
        };
    }
}
