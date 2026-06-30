package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(PurchaseOrder.class)
public class PurchaseOrder_ {
    public static volatile SingularAttribute<PurchaseOrder, Long> id;
    public static volatile SingularAttribute<PurchaseOrder, String> purchaseOrderNumber;
    public static volatile SingularAttribute<PurchaseOrder, String> externalId;
    public static volatile SingularAttribute<PurchaseOrder, Long> purchaseRequisitionId;
    public static volatile SingularAttribute<PurchaseOrder, Long> organizationId;
    public static volatile SingularAttribute<PurchaseOrder, Long> supplierId;
    public static volatile SingularAttribute<PurchaseOrder, String> buyerContact;
    public static volatile SingularAttribute<PurchaseOrder, String> supplierContact;
    public static volatile SingularAttribute<PurchaseOrder, String> currency;
    public static volatile SingularAttribute<PurchaseOrder, PaymentTerm> paymentTerm;
    public static volatile SingularAttribute<PurchaseOrder, LocalDate> paymentDueDate;
    public static volatile SingularAttribute<PurchaseOrder, BigDecimal> totalAmount;
    public static volatile SingularAttribute<PurchaseOrder, PurchaseOrderStatus> status;
    public static volatile SingularAttribute<PurchaseOrder, LocalDate> orderDate;
    public static volatile SingularAttribute<PurchaseOrder, LocalDate> expectedDate;
    public static volatile SingularAttribute<PurchaseOrder, LocalDateTime> receivedDate;
    public static volatile SingularAttribute<PurchaseOrder, Long> createdByUserId;
    public static volatile SingularAttribute<PurchaseOrder, Long> updatedByUserId;
    public static volatile SingularAttribute<PurchaseOrder, LocalDateTime> createdAt;
    public static volatile SingularAttribute<PurchaseOrder, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<PurchaseOrder, EntityStatus> entityStatus;
    public static volatile SingularAttribute<PurchaseOrder, String> notes;
    public static volatile ListAttribute<PurchaseOrder, PurchaseOrderLine> purchaseOrderLines;
}
