package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.Product_;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class ProductSpecification {

    public static Specification<Product> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.notLike(root.get(Product_.entityStatus).as(String.class), "%" + entityStatus + "%");
            return p;
        };
    }

    public static Specification<Product> nameLike(final String name) {
        return (root, query, cb) -> {
            Predicate p = cb.like(root.get(Product_.name).as(String.class), name + "%");
            return p;
        };
    }

    public static Specification<Product> productCodeEquals(final String productCode) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Product_.productCode).as(String.class), productCode);
            return p;
        };
    }

    public static Specification<Product> barcodeEquals(final String barcode) {
        return (root, query, cb) -> cb.equal(root.get(Product_.barcode).as(String.class), barcode);
    }

    public static Specification<Product> unitOfMeasureEquals(final String unitOfMeasure) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Product_.unitOfMeasure).as(String.class), unitOfMeasure);
            return p;
        };
    }

    public static Specification<Product> any(final String search) {
        return (root, query, cb) -> {
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(Product_.name)), "%" + search.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get(Product_.description)), "%" + search.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get(Product_.productCode)), "%" + search.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get(Product_.barcode)), "%" + search.toUpperCase() + "%"),
                    cb.like(cb.upper(root.get(Product_.manufacturer)), "%" + search.toUpperCase() + "%")
            );
            return p;
        };
    }

    public static Specification<Product> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get(Product_.entityStatus), entityStatus);
            return p;
        };
    }

    public static Specification<Product> supplierIdEquals(final Long supplierId) {
        return (root, query, cb) -> cb.equal(root.get(Product_.supplierId), supplierId);
    }
}
