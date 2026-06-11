package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(WarehouseLocation.class)
public class WarehouseLocation_ {
    public static volatile SingularAttribute<WarehouseLocation, String> locationId;
    public static volatile SingularAttribute<WarehouseLocation, String> name;
    public static volatile SingularAttribute<WarehouseLocation, String> description;
    public static volatile SingularAttribute<WarehouseLocation, Long> supplierId;
    public static volatile SingularAttribute<WarehouseLocation, String> entityStatus;
}
