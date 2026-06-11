package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(InventoryTransfer.class)
public class InventoryTransfer_ {
    public static volatile SingularAttribute<InventoryTransfer, Double> quantity;
    public static volatile SingularAttribute<InventoryTransfer, TransferStatus> status;
    public static volatile SingularAttribute<InventoryTransfer, String> reference;
    public static volatile SingularAttribute<InventoryTransfer, String> entityStatus;
}
