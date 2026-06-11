package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(PurchaseOrder.class)
public class PurchaseOrder_ {
    public static volatile SingularAttribute<PurchaseOrder, Long> id;
    public static volatile SingularAttribute<PurchaseOrder, String> purchaseOrderNumber;
    public static volatile SingularAttribute<PurchaseOrder, String> externalId;
    public static volatile SingularAttribute<PurchaseOrder, PurchaseOrderStatus> status;
    public static volatile SingularAttribute<PurchaseOrder, LocalDate> orderDate;
    public static volatile SingularAttribute<PurchaseOrder, LocalDate> expectedDate;
    public static volatile SingularAttribute<PurchaseOrder, LocalDateTime> receivedDate;
    public static volatile SingularAttribute<PurchaseOrder, String> notes;
    public static volatile SingularAttribute<PurchaseOrder, LocalDateTime> createdAt;
    public static volatile SingularAttribute<PurchaseOrder, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<PurchaseOrder, EntityStatus> entityStatus;
}
