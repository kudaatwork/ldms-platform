package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.model.Suburb_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class SuburbSpecification {

    public static Specification<Suburb> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(Suburb_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<Suburb> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Suburb_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<Suburb> codeLike(final String code) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Suburb_.code).as(String.class), code + "%");
            return p;
        };
    }

    public static Specification<Suburb> postalCodeLike(final String postalCode) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Suburb_.postalCode).as(String.class), postalCode + "%");
            return p;
        };
    }

    public static Specification<Suburb> byDistrict(final Long districtId) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Suburb_.district).get("id"), districtId);
            return p;
        };
    }

    public static Specification<Suburb> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(Suburb_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Suburb_.code), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Suburb_.postalCode), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<Suburb> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Suburb_.entityStatus), entityStatus);
            return p;
        };
    }
}