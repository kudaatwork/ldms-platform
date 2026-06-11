package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.ProductSubCategory;
import projectlx.inventory.management.model.ProductSubCategory_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class ProductSubCategorySpecification {

    public static Specification<ProductSubCategory> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(ProductSubCategory_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<ProductSubCategory> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(ProductSubCategory_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<ProductSubCategory> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(ProductSubCategory_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<ProductSubCategory> categoryIdEquals(final Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<ProductSubCategory> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(root.get(ProductSubCategory_.name), "%" + upper + "%"),
                    cb.like(root.get(ProductSubCategory_.description), "%" + upper + "%"),
                    cb.equal(root.get(ProductSubCategory_.entityStatus), "%" + upper + "%")
            );
            return p;
        };
    }
}
