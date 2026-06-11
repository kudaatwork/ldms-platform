package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseLocation_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class WarehouseLocationSpecification {

    public static Specification<WarehouseLocation> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(WarehouseLocation_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<WarehouseLocation> nameLike(final String name) {
        return (root, query, cb) -> {
            // Using literal attribute name because WarehouseLocation_ does not expose 'name'
            Predicate p = cb.like(root.get(WarehouseLocation_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<WarehouseLocation> descriptionLike(final String description) {
        return (root, query, cb) -> {
            // Using literal attribute name because WarehouseLocation_ does not expose 'description'
            Predicate p = cb.like(root.get(WarehouseLocation_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<WarehouseLocation> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    // Using literal attribute names for 'name' and 'description'
                    cb.like(root.get(WarehouseLocation_.name), "%" + upper + "%"),
                    cb.like(root.get(WarehouseLocation_.description), "%" + upper + "%"),
                    cb.equal(root.get(WarehouseLocation_.entityStatus), "%" + upper + "%")
            );
            return p;
        };
    }
}
