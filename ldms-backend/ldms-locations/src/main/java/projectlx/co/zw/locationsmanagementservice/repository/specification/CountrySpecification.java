package projectlx.co.zw.locationsmanagementservice.repository.specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.Country_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class CountrySpecification {

    /**
     * Exclude rows whose status is {@code excludedStatus}, matching case-insensitively on the DB string
     * (imports / legacy data may use {@code deleted} vs {@code DELETED}). NULL/blank is treated as not deleted.
     */
    public static Specification<Country> deleted(EntityStatus excludedStatus) {
        final String excluded = excludedStatus.name();
        return (root, query, cb) -> {
            var path = root.get(Country_.entityStatus);
            Expression<String> raw = path.as(String.class);
            Expression<String> norm = cb.upper(cb.trim(cb.coalesce(raw, cb.literal(""))));
            // Explicit IS NULL: some SQL paths treat UPPER(TRIM(NULL)) as unknown and would drop rows otherwise.
            return cb.or(cb.isNull(path), cb.notEqual(norm, excluded));
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
        final String target = entityStatus.name();
        return (root, query, cb) -> {
            Expression<String> raw = root.get(Country_.entityStatus).as(String.class);
            Expression<String> norm = cb.upper(cb.trim(cb.coalesce(raw, cb.literal(""))));
            // Legacy NULL = treat as ACTIVE when filtering ACTIVE. Non-canonical casing on the column still matches.
            if (entityStatus == EntityStatus.ACTIVE) {
                return cb.or(
                        cb.isNull(root.get(Country_.entityStatus)),
                        cb.equal(norm, target));
            }
            return cb.equal(norm, target);
        };
    }
}
