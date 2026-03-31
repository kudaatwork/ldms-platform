package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.Country_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class CountrySpecification {

    public static Specification<Country> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(Country_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<Country> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Country_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<Country> isoAlpha2CodeLike(final String isoAlpha2Code) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Country_.isoAlpha2Code).as(String.class), isoAlpha2Code + "%");
            return p;
        };
    }

    public static Specification<Country> isoAlpha3CodeLike(final String isoAlpha3Code) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Country_.isoAlpha3Code).as(String.class), isoAlpha3Code + "%");
            return p;
        };
    }

    public static Specification<Country> dialCodeLike(final String dialCode) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Country_.dialCode).as(String.class), dialCode + "%");
            return p;
        };
    }

    public static Specification<Country> timezoneLike(final String timezone) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Country_.timezone).as(String.class), timezone + "%");
            return p;
        };
    }

    public static Specification<Country> currencyCodeLike(final String currencyCode) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Country_.currencyCode).as(String.class), currencyCode + "%");
            return p;
        };
    }

    public static Specification<Country> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(Country_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Country_.isoAlpha2Code), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Country_.isoAlpha3Code), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Country_.dialCode), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Country_.timezone), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Country_.currencyCode), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<Country> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Country_.entityStatus), entityStatus);
            return p;
        };
    }
}
