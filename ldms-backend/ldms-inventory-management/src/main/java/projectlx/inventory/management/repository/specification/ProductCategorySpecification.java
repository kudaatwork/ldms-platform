package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.ProductCategory;
import projectlx.inventory.management.model.ProductCategory_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class ProductCategorySpecification {

    public static Specification<ProductCategory> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(ProductCategory_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<ProductCategory> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(ProductCategory_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<ProductCategory> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(ProductCategory_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<ProductCategory> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(root.get(ProductCategory_.name), "%" + search.toUpperCase() + "%"),
                    cb.like(root.get(ProductCategory_.description), "%" + search.toUpperCase() + "%"),
                    cb.equal(root.get(ProductCategory_.entityStatus), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<ProductCategory> supplierIdEquals(final Long supplierId) {
        return (root, query, cb) -> cb.equal(root.get(ProductCategory_.supplierId), supplierId);
    }
}
