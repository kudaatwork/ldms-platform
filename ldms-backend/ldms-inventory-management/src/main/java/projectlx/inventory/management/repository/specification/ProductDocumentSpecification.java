package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.ProductDocument;
import projectlx.inventory.management.model.ProductDocument_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class ProductDocumentSpecification {

    public static Specification<ProductDocument> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(ProductDocument_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<ProductDocument> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(ProductDocument_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<ProductDocument> descriptionLike(final String description) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(ProductDocument_.description).as(String.class), description + "%");
            return p;
        };
    }

    public static Specification<ProductDocument> documentIdEquals(final String documentId) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(ProductDocument_.documentId).as(String.class), documentId);
            return p;
        };
    }

    public static Specification<ProductDocument> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(ProductDocument_.name)), "%" + search.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get(ProductDocument_.description)), "%" + search.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get(ProductDocument_.documentId)), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<ProductDocument> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(ProductDocument_.entityStatus), entityStatus);
            return p;
        };
    }
}
