package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import projectlx.co.zw.locationsmanagementservice.model.LocalizedName_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class LocalizedNameSpecification {

    public static Specification<LocalizedName> deleted(EntityStatus excludedStatus) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(LocalizedName_.entityStatus)),
                cb.notEqual(root.get(LocalizedName_.entityStatus), excludedStatus));
    }

    public static Specification<LocalizedName> valueLike(final String value) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(LocalizedName_.value).as(String.class), value + "%");
            return p;
        };
    }

    public static Specification<LocalizedName> byLanguage(final Long languageId) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(LocalizedName_.language).get("id"), languageId);
            return p;
        };
    }

    public static Specification<LocalizedName> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(LocalizedName_.value), "%" + search.toUpperCase() + "%");
            return p;
        };
    }

    public static Specification<LocalizedName> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(LocalizedName_.entityStatus), entityStatus);
            return p;
        };
    }
}
