package projectlx.co.zw.locationsmanagementservice.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.locationsmanagementservice.model.Village;
import projectlx.co.zw.locationsmanagementservice.model.Village_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class VillageSpecification {

    public static Specification<Village> deleted(EntityStatus excludedStatus) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(Village_.entityStatus)),
                cb.notEqual(root.get(Village_.entityStatus), excludedStatus));
    }

    public static Specification<Village> nameLike(final String name) {
        return (root, query, cb) -> cb.like(root.get(Village_.name).as(String.class), name + "%");
    }

    public static Specification<Village> codeLike(final String code) {
        return (root, query, cb) -> cb.like(root.get(Village_.code).as(String.class), code + "%");
    }

    public static Specification<Village> byDistrict(final Long districtId) {
        return (root, query, cb) -> cb.equal(root.get(Village_.district).get("id"), districtId);
    }

    public static Specification<Village> byCity(final Long cityId) {
        return (root, query, cb) -> cb.equal(root.get(Village_.city).get("id"), cityId);
    }

    public static Specification<Village> any(final String search) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.upper(root.get(Village_.name)), "%" + search.toUpperCase() + "%"),
                cb.like(cb.upper(root.get(Village_.code)), "%" + search.toUpperCase() + "%")
        );
    }

    public static Specification<Village> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(root.get(Village_.entityStatus), entityStatus);
    }
}
