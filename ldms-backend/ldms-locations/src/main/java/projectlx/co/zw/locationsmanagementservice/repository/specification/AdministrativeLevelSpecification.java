package projectlx.co.zw.locationsmanagementservice.repository.specification;

import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class AdministrativeLevelSpecification {

    public static Specification<AdministrativeLevel> deleted(EntityStatus excludedStatus) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(AdministrativeLevel_.entityStatus)),
                cb.notEqual(root.get(AdministrativeLevel_.entityStatus), excludedStatus));
    }

    public static Specification<AdministrativeLevel> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(AdministrativeLevel_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<AdministrativeLevel> codeLike(final String code) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(AdministrativeLevel_.code).as(String.class), code + "%");
            return p;
        };
    }

    public static Specification<AdministrativeLevel> byLevel(final Integer level) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(AdministrativeLevel_.level), level);
            return p;
        };
    }

    public static Specification<AdministrativeLevel> byCountry(final Long countryId) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(AdministrativeLevel_.country).get("id"), countryId);
            return p;
        };
    }

    public static Specification<AdministrativeLevel> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(AdministrativeLevel_.description).as(String.class), "%" + description + "%");
            return p;
        };
    }

    public static Specification<AdministrativeLevel> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(AdministrativeLevel_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(AdministrativeLevel_.code), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(AdministrativeLevel_.description), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<AdministrativeLevel> hasEntityStatus(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(AdministrativeLevel_.entityStatus), entityStatus);
            return p;
        };
    }
}