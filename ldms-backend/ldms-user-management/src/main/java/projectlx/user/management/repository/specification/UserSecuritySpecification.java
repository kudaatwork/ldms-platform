package projectlx.user.management.repository.specification;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserSecurity;
import projectlx.user.management.model.UserSecurity_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserSecuritySpecification {

    public static Specification<UserSecurity> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(UserSecurity_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<UserSecurity> isTwoFactorEnabledLike(final Boolean isTwoFactorEnabled) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(UserSecurity_.isTwoFactorEnabled).as(Boolean.class), isTwoFactorEnabled + "%");
            return p;
        };
    }

    public static Specification<UserSecurity> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(UserSecurity_.securityQuestion_1), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserSecurity_.securityAnswer_1), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserSecurity_.securityQuestion_2), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserSecurity_.securityAnswer_2), "%" + search.toUpperCase() + "%"),
                    cb.equal(root.get(UserSecurity_.isTwoFactorEnabled), "%" + search.toUpperCase() + "%")

            );

            return p;
        };
    }
}
