package projectlx.inventory.management.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.model.InventoryTransfer;
import projectlx.inventory.management.model.InventoryTransfer_;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.TransferStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

public class InventoryTransferSpecification {

    public static Specification<InventoryTransfer> deleted(EntityStatus entityStatus) {
        return (root, query, cb) -> cb.notEqual(
                root.get("entityStatus"), entityStatus);
    }

    public static Specification<InventoryTransfer> deleted() {
        return (root, query, cb) -> cb.notEqual(
                root.get("entityStatus"), EntityStatus.DELETED);
    }

    public static Specification<InventoryTransfer> productEquals(final Product product) {
        return (root, query, cb) -> cb.equal(
                root.get("product"), product);
    }
    
    public static Specification<InventoryTransfer> productIdEquals(final Long productId) {
        return (root, query, cb) -> cb.equal(
                root.get("product").get("id"), productId);
    }

    public static Specification<InventoryTransfer> fromLocationEquals(final WarehouseLocation fromLocation) {
        return (root, query, cb) -> cb.equal(
                root.get("fromLocation"), fromLocation);
    }
    
    public static Specification<InventoryTransfer> fromLocationIdEquals(final Long fromLocationId) {
        return (root, query, cb) -> cb.equal(
                root.get("fromLocation").get("id"), fromLocationId);
    }

    public static Specification<InventoryTransfer> toLocationEquals(final WarehouseLocation toLocation) {
        return (root, query, cb) -> cb.equal(
                root.get("toLocation"), toLocation);
    }
    
    public static Specification<InventoryTransfer> toLocationIdEquals(final Long toLocationId) {
        return (root, query, cb) -> cb.equal(
                root.get("toLocation").get("id"), toLocationId);
    }

    public static Specification<InventoryTransfer> transferNumberLike(final String transferNumber) {
        return (root, query, cb) -> cb.like(
                root.get("transferNumber"), transferNumber + "%");
    }

    public static Specification<InventoryTransfer> transferNumberEquals(final String transferNumber) {
        return (root, query, cb) -> cb.equal(
                root.get("transferNumber"), transferNumber);
    }

    public static Specification<InventoryTransfer> referenceLike(final String reference) {
        return (root, query, cb) -> cb.like(
                root.get(InventoryTransfer_.reference), reference + "%");
    }

    public static Specification<InventoryTransfer> statusEquals(final TransferStatus status) {
        return (root, query, cb) -> cb.equal(
                root.get(InventoryTransfer_.status), status);
    }

    public static Specification<InventoryTransfer> entityStatusEquals(final EntityStatus entityStatus) {
        return (root, query, cb) -> cb.equal(
                root.get("entityStatus"), entityStatus);
    }

    public static Specification<InventoryTransfer> createdByUserIdEquals(final Long createdByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get("createdByUserId"), createdByUserId);
    }

    public static Specification<InventoryTransfer> updatedByUserIdEquals(final Long updatedByUserId) {
        return (root, query, cb) -> cb.equal(
                root.get("updatedByUserId"), updatedByUserId);
    }

    public static Specification<InventoryTransfer> createdAtBefore(final LocalDateTime createdAt) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get("createdAt"), createdAt);
    }

    public static Specification<InventoryTransfer> createdAtAfter(final LocalDateTime createdAt) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get("createdAt"), createdAt);
    }

    public static Specification<InventoryTransfer> updatedAtBefore(final LocalDateTime updatedAt) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(
                root.get("updatedAt"), updatedAt);
    }

    public static Specification<InventoryTransfer> updatedAtAfter(final LocalDateTime updatedAt) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.get("updatedAt"), updatedAt);
    }

    public static Specification<InventoryTransfer> any(final String search) {
        return (root, query, cb) -> {
            String upper = search == null ? "" : search.toUpperCase();
            Predicate p = cb.or(
                    cb.like(cb.upper(root.get("transferNumber")), "%" + upper + "%"),
                    cb.like(cb.upper(root.get(InventoryTransfer_.reference)), "%" + upper + "%")
            );
            return p;
        };
    }
}