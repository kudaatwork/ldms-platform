package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.PurchaseOrderLine;
import projectlx.inventory.management.model.PurchaseOrderLine_;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class PurchaseOrderLineSpecification {

    // Match style of existing specs while fitting current service usage (no-arg deleted)
    public static Specification<PurchaseOrderLine> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get(PurchaseOrderLine_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<PurchaseOrderLine> unitOfMeasureEquals(final UnitOfMeasure unitOfMeasure) {
        return (root, query, cb) -> cb.equal(root.get(PurchaseOrderLine_.unitOfMeasure), unitOfMeasure);
    }

    public static Specification<PurchaseOrderLine> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(root.get(PurchaseOrderLine_.entityStatus), entityStatus);
    }

    public static Specification<PurchaseOrderLine> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(PurchaseOrderLine_.supplierProductCode)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get("product").get("name")), "%" + upper + "%")
            );
            return p;
        };
    }
}
