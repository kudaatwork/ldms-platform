package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.Language;
import projectlx.co.zw.locationsmanagementservice.model.Language_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class LanguageSpecification {

    public static Specification<Language> deleted(EntityStatus excludedStatus) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(Language_.entityStatus)),
                cb.notEqual(root.get(Language_.entityStatus), excludedStatus));
    }

    public static Specification<Language> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Language_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<Language> isoCodeLike(final String isoCode) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Language_.isoCode).as(String.class), isoCode + "%");
            return p;
        };
    }

    public static Specification<Language> nativeNameLike(final String nativeName) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Language_.nativeName).as(String.class), nativeName + "%");
            return p;
        };
    }


    public static Specification<Language> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(Language_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Language_.isoCode), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Language_.nativeName), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<Language> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Language_.entityStatus), entityStatus);
            return p;
        };
    }
}
