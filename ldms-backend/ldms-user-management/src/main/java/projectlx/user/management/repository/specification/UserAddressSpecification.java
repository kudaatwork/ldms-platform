package projectlx.user.management.repository.specification;

import projectlx.user.management.model.Address;
import projectlx.user.management.model.Address_;
import projectlx.user.management.model.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserAddressSpecification {

    public static Specification<Address> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(Address_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<Address> streetAddressLike(final String streetAddress) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.streetAddress).as(String.class), streetAddress + "%");
            return p;
        };
    }

    public static Specification<Address> cityLike(final String city) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.city).as(String.class), city + "%");
            return p;
        };
    }

    public static Specification<Address> stateLike(final String state) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.state).as(String.class), state + "%");
            return p;
        };
    }

    public static Specification<Address> postalCodeLike(final String postalCode) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.postalCode).as(String.class), postalCode + "%");
            return p;
        };
    }

    public static Specification<Address> countryLike(final String country) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Address_.country).as(String.class), country + "%");
            return p;
        };
    }

    public static Specification<Address> any(final String search) {

        return (root, query, cb) -> {

            Predicate p = cb.or(
                    cb.like(root.get(Address_.streetAddress), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Address_.city), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Address_.state), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Address_.postalCode), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Address_.country), "%" + search.toUpperCase() + "%")
            );

            return p;
        };
    }
}
