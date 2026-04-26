package projectlx.user.management.repository.specification;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserAccount;
import projectlx.user.management.model.UserAccount_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserAccountSpecification {

    public static Specification<UserAccount> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(UserAccount_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<UserAccount> phoneNumberLike(final String phoneNumber) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserAccount_.phoneNumber).as(String.class), phoneNumber + "%");
            return p;
        };
    }

    public static Specification<UserAccount> accountNumberLike(final String accountNumber) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(UserAccount_.accountNumber).as(String.class), accountNumber + "%");
            return p;
        };
    }

    public static Specification<UserAccount> isAccountLocked(final Boolean isAccountLocked) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(UserAccount_.isAccountLocked).as(Boolean.class), isAccountLocked + "%");
            return p;
        };
    }

    public static Specification<UserAccount> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(UserAccount_.phoneNumber), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(UserAccount_.accountNumber), "%" + search.toUpperCase() + "%"),
                    cb.equal(root.get(UserAccount_.isAccountLocked), "%" + search.toUpperCase() + "%")
            );

            return p;
        };
    }
}
