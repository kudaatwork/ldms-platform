package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(PurchaseReturn.class)
public class PurchaseReturn_ {
    public static volatile SingularAttribute<PurchaseReturn, Long> id;
    public static volatile SingularAttribute<PurchaseReturn, String> returnNumber;
    public static volatile SingularAttribute<PurchaseReturn, PurchaseOrder> purchaseOrder;
    public static volatile SingularAttribute<PurchaseReturn, WarehouseLocation> warehouseLocation;
    public static volatile SingularAttribute<PurchaseReturn, Long> returnedByUserId;
    public static volatile SingularAttribute<PurchaseReturn, String> reason;
    public static volatile SingularAttribute<PurchaseReturn, LocalDateTime> createdAt;
    public static volatile SingularAttribute<PurchaseReturn, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<PurchaseReturn, EntityStatus> entityStatus;
}