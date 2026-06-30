package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.inventory.management.model.Department;
import projectlx.inventory.management.model.Department_;

public class DepartmentSpecification {

    public static Specification<Department> deleted(EntityStatus entityStatus) {
        return (root, query, cb) ->
                cb.notLike(root.get(Department_.entityStatus).as(String.class), "%" + entityStatus + "%");
    }

    public static Specification<Department> nameLike(final String name) {
        return (root, query, cb) -> cb.like(root.get(Department_.name).as(String.class), name + "%");
    }

    public static Specification<Department> departmentCodeLike(final String departmentCode) {
        return (root, query, cb) ->
                cb.like(root.get(Department_.departmentCode).as(String.class), departmentCode + "%");
    }

    public static Specification<Department> descriptionLike(final String description) {
        return (root, query, cb) ->
                cb.like(root.get(Department_.description).as(String.class), description + "%");
    }

    public static Specification<Department> any(final String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toUpperCase() + "%";
            Predicate p = cb.or(
                    cb.like(root.get(Department_.name), pattern),
                    cb.like(root.get(Department_.departmentCode), pattern),
                    cb.like(root.get(Department_.description), pattern)
            );
            return p;
        };
    }

    public static Specification<Department> supplierIdEquals(final Long supplierId) {
        return (root, query, cb) -> cb.equal(root.get(Department_.supplierId), supplierId);
    }
}
