package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(SalesOrderLine.class)
public class SalesOrderLine_ {
    public static volatile SingularAttribute<SalesOrderLine, Long> id;
    public static volatile SingularAttribute<SalesOrderLine, SalesOrder> salesOrder;
    public static volatile SingularAttribute<SalesOrderLine, Product> product;
    public static volatile SingularAttribute<SalesOrderLine, BigDecimal> quantity;
    public static volatile SingularAttribute<SalesOrderLine, BigDecimal> unitPrice;
    public static volatile SingularAttribute<SalesOrderLine, BigDecimal> totalPrice;
    public static volatile SingularAttribute<SalesOrderLine, BigDecimal> fulfilledQuantity;
    public static volatile SingularAttribute<SalesOrderLine, UnitOfMeasure> unitOfMeasure;
    public static volatile SingularAttribute<SalesOrderLine, Long> createdByUserId;
    public static volatile SingularAttribute<SalesOrderLine, Long> updatedByUserId;
    // BaseEntity fields
    public static volatile SingularAttribute<SalesOrderLine, LocalDateTime> createdAt;
    public static volatile SingularAttribute<SalesOrderLine, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<SalesOrderLine, EntityStatus> entityStatus;
    public static volatile SingularAttribute<SalesOrderLine, Long> version;
}
