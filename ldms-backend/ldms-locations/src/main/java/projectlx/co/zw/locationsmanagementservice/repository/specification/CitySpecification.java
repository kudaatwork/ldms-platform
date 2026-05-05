package projectlx.co.zw.locationsmanagementservice.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.model.City_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class CitySpecification {

    public static Specification<City> deleted(EntityStatus excludedStatus) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(City_.entityStatus)),
                cb.notEqual(root.get(City_.entityStatus), excludedStatus));
    }

    public static Specification<City> nameLike(final String name) {
        return (root, query, cb) -> cb.like(root.get(City_.name).as(String.class), name + "%");
    }

    public static Specification<City> codeLike(final String code) {
        return (root, query, cb) -> cb.like(root.get(City_.code).as(String.class), code + "%");
    }

    public static Specification<City> byDistrict(final Long districtId) {
        return (root, query, cb) -> cb.equal(root.get(City_.district).get("id"), districtId);
    }

    public static Specification<City> any(final String search) {
        return (root, query, cb) -> cb.or(
                cb.like(cb.upper(root.get(City_.name)), "%" + search.toUpperCase() + "%"),
                cb.like(cb.upper(root.get(City_.code)), "%" + search.toUpperCase() + "%")
        );
    }

    public static Specification<City> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(root.get(City_.entityStatus), entityStatus);
    }
}
