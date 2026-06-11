package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Metamodel for PurchaseRequisitionLine entity.
 */
@StaticMetamodel(PurchaseRequisitionLine.class)
public class PurchaseRequisitionLine_ {
    public static volatile SingularAttribute<PurchaseRequisitionLine, Long> id;
    public static volatile SingularAttribute<PurchaseRequisitionLine, PurchaseRequisition> purchaseRequisition;
    public static volatile SingularAttribute<PurchaseRequisitionLine, Integer> lineNumber;
    public static volatile SingularAttribute<PurchaseRequisitionLine, Product> product;
    public static volatile SingularAttribute<PurchaseRequisitionLine, String> productDescription;
    public static volatile SingularAttribute<PurchaseRequisitionLine, UnitOfMeasure> unitOfMeasure;

    // Quantity tracking
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> requestedQuantity;
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> approvedQuantity;
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> orderedQuantity;
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> fulfilledFromStockQuantity;
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> fulfilledFromTransferQuantity;
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> remainingQuantity;

    // Pricing
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> estimatedUnitPrice;
    public static volatile SingularAttribute<PurchaseRequisitionLine, BigDecimal> estimatedTotalPrice;

    // Fulfillment
    public static volatile SingularAttribute<PurchaseRequisitionLine, FulfillmentMethod> fulfillmentMethod;
    public static volatile SingularAttribute<PurchaseRequisitionLine, String> fulfillmentNotes;

    // Audit fields
    public static volatile SingularAttribute<PurchaseRequisitionLine, Long> createdByUserId;
    public static volatile SingularAttribute<PurchaseRequisitionLine, Long> updatedByUserId;
    public static volatile SingularAttribute<PurchaseRequisitionLine, LocalDateTime> createdAt;
    public static volatile SingularAttribute<PurchaseRequisitionLine, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<PurchaseRequisitionLine, EntityStatus> entityStatus;
}
