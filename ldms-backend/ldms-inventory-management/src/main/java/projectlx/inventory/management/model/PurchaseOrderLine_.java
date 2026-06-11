package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(PurchaseOrderLine.class)
public class PurchaseOrderLine_ {
    public static volatile SingularAttribute<PurchaseOrderLine, String> supplierProductCode;
    public static volatile SingularAttribute<PurchaseOrderLine, UnitOfMeasure> unitOfMeasure;
    public static volatile SingularAttribute<PurchaseOrderLine, BigDecimal> quantity;
    public static volatile SingularAttribute<PurchaseOrderLine, BigDecimal> unitPrice;
    public static volatile SingularAttribute<PurchaseOrderLine, BigDecimal> totalPrice;
    public static volatile SingularAttribute<PurchaseOrderLine, BigDecimal> receivedQuantity;
    public static volatile SingularAttribute<PurchaseOrderLine, LocalDateTime> createdAt;
    public static volatile SingularAttribute<PurchaseOrderLine, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<PurchaseOrderLine, EntityStatus> entityStatus;
}
