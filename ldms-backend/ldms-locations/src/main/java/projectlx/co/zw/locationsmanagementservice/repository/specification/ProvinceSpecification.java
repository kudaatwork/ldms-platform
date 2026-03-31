package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.model.Province_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ProvinceSpecification {

    public static Specification<Province> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(Province_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<Province> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Province_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<Province> codeLike(final String code) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Province_.code).as(String.class), code + "%");
            return p;
        };
    }

    public static Specification<Province> byCountry(final Long countryId) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Province_.country).get("id"), countryId);
            return p;
        };
    }

    public static Specification<Province> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(Province_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(Province_.code), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<Province> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Province_.entityStatus), entityStatus);
            return p;
        };
    }
}