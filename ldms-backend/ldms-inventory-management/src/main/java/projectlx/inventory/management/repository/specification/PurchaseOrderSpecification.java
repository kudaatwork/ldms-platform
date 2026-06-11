package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.PurchaseOrder_;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDate;

public class PurchaseOrderSpecification {

    // Provide both signatures: consistent with existing style and convenient default
    public static Specification<PurchaseOrder> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notLike(root.get(PurchaseOrder_.entityStatus).as(String.class), "%" + entityStatus + "%");
    }

    public static Specification<PurchaseOrder> deleted() {
        return (root, query, cb) -> cb.notEqual(root.get(PurchaseOrder_.entityStatus), EntityStatus.DELETED);
    }

    public static Specification<PurchaseOrder> purchaseOrderNumberLike(final String purchaseOrderNumber) {
        return (root, query, cb) -> cb.like(root.get(PurchaseOrder_.purchaseOrderNumber).as(String.class), purchaseOrderNumber + "%");
    }

    public static Specification<PurchaseOrder> externalIdLike(final String externalId) {
        return (root, query, cb) -> cb.like(root.get(PurchaseOrder_.externalId).as(String.class), externalId + "%");
    }

    public static Specification<PurchaseOrder> statusEquals(final PurchaseOrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get(PurchaseOrder_.status), status);
    }

    public static Specification<PurchaseOrder> orderDateEquals(final LocalDate orderDate) {
        return (root, query, cb) -> cb.equal(root.get(PurchaseOrder_.orderDate), orderDate);
    }

    public static Specification<PurchaseOrder> expectedDateEquals(final LocalDate expectedDate) {
        return (root, query, cb) -> cb.equal(root.get(PurchaseOrder_.expectedDate), expectedDate);
    }

    public static Specification<PurchaseOrder> notesLike(final String notes) {
        return (root, query, cb) -> cb.like(root.get(PurchaseOrder_.notes).as(String.class), notes + "%");
    }

    public static Specification<PurchaseOrder> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(root.get(PurchaseOrder_.entityStatus), entityStatus);
    }

    public static Specification<PurchaseOrder> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get(PurchaseOrder_.purchaseOrderNumber)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(PurchaseOrder_.externalId)), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(PurchaseOrder_.notes)), "%" + upper + "%")
            );
            return p;
        };
    }
}
