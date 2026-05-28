package projectlx.co.zw.organizationmanagement.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.ArrayList;

public final class IndustrySpecifications {

    private IndustrySpecifications() {
    }

    public static Specification<Industry> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED);
    }

    public static Specification<Industry> nameLike(String name) {
        String pattern = "%" + name.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Industry> industryCodeLike(String industryCode) {
        String pattern = "%" + industryCode.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(cb.coalesce(root.get("industryCode"), cb.literal(""))), pattern);
    }

    public static Specification<Industry> searchValueLike(String searchValue) {
        String pattern = "%" + searchValue.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("industryCode"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("description"), cb.literal(""))), pattern));
            predicates.add(cb.like(cb.lower(cb.coalesce(root.get("regulatoryBodyName"), cb.literal(""))), pattern));
            return cb.or(predicates.toArray(Predicate[]::new));
        };
    }
}
