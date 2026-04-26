package projectlx.user.management.repository.specification;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.Gender;
import projectlx.user.management.model.User;
import projectlx.user.management.model.User_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

public class UserSpecification {

    public static Specification<User> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(User_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<User> firstNameLike(final String firstName) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(User_.firstName).as(String.class), firstName + "%");
            return p;
        };
    }

    public static Specification<User> lastNameLike(final String lastName) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(User_.lastName).as(String.class), lastName + "%");
            return p;
        };
    }

    public static Specification<User> userNameLike(final String userName) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(User_.username).as(String.class), userName + "%");
            return p;
        };
    }

    public static Specification<User> emailLike(final String email) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(User_.email).as(String.class), email + "%");
            return p;
        };
    }

    public static Specification<User> genderIn(final List<Gender> genderList) {
        return (root, query, cb) -> root.get(User_.gender).in(genderList);
    }

    public static Specification<User> phoneNumberLike(final String phoneNumber) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(User_.phoneNumber).as(String.class), phoneNumber + "%");
            return p;
        };
    }

    public static Specification<User> nationalIdNumberLike(final String nationalIdNumber) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(User_.nationalIdNumber).as(String.class), nationalIdNumber + "%");
            return p;
        };
    }

    public static Specification<User> passportNumberLike(final String passportNumber) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(User_.passportNumber).as(String.class), passportNumber + "%");
            return p;
        };
    }

    public static Specification<User> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(User_.firstName), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(User_.lastName), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(User_.username), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(User_.email), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(User_.phoneNumber), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(User_.nationalIdNumber), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(User_.passportNumber), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(User_.gender), "%" + search.toUpperCase() + "%")
            );

            try {

                Gender gender = Gender.valueOf(search);

                p = cb.or(p, cb.equal(root.get(User_.gender), gender));
            }
            catch (Exception e) {

                p = cb.or(
                        cb.like(root.get(User_.firstName), "%" + search.toUpperCase() + "%"),
                        cb.like(root.get(User_.lastName), "%" + search.toUpperCase() + "%"),
                        cb.like(root.get(User_.username), "%" + search.toUpperCase() + "%"),
                        cb.like(root.get(User_.email), "%" + search.toUpperCase() + "%"),
                        cb.like(root.get(User_.phoneNumber), "%" + search.toUpperCase() + "%"),
                        cb.like(root.get(User_.nationalIdNumber), "%" + search.toUpperCase() + "%"),
                        cb.like(root.get(User_.phoneNumber), "%" + search.toUpperCase() + "%"),
                        cb.like(root.get(User_.gender), "%" + search.toUpperCase() + "%"));
            }

            return p;
        };
    }
}
