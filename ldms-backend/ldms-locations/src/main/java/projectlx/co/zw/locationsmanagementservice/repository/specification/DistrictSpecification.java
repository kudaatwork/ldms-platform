package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.District_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class DistrictSpecification {

    public static Specification<District> deleted(EntityStatus excludedStatus) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(District_.entityStatus)),
                cb.notEqual(root.get(District_.entityStatus), excludedStatus));
    }

    public static Specification<District> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(District_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<District> codeLike(final String code) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(District_.code).as(String.class), code + "%");
            return p;
        };
    }

    public static Specification<District> byProvince(final Long provinceId) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(District_.province).get("id"), provinceId);
            return p;
        };
    }

    public static Specification<District> byAdministrativeLevel(final Long administrativeLevelId) {
        return (root, query, cb) -> cb.equal(root.get(District_.administrativeLevel).get("id"), administrativeLevelId);
    }

    public static Specification<District> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(District_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(District_.code), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<District> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(District_.entityStatus), entityStatus);
            return p;
        };
    }
}