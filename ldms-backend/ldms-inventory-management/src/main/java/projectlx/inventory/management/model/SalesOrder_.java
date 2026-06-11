package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(SalesOrder.class)
public class SalesOrder_ {
    public static volatile SingularAttribute<SalesOrder, Long> id;
    public static volatile SingularAttribute<SalesOrder, String> salesOrderNumber;
    public static volatile SingularAttribute<SalesOrder, Long> customerId;
    public static volatile SingularAttribute<SalesOrder, Long> supplierOrganizationId;
    public static volatile SingularAttribute<SalesOrder, SalesOrderStatus> status;
    public static volatile SingularAttribute<SalesOrder, LocalDate> orderDate;
    public static volatile SingularAttribute<SalesOrder, LocalDate> expectedDeliveryDate;
    public static volatile SingularAttribute<SalesOrder, LocalDateTime> deliveredDate;
    public static volatile SingularAttribute<SalesOrder, BigDecimal> totalAmount;
    public static volatile SingularAttribute<SalesOrder, String> notes;
    public static volatile SingularAttribute<SalesOrder, Long> createdByUserId;
    public static volatile SingularAttribute<SalesOrder, Long> updatedByUserId;
    public static volatile ListAttribute<SalesOrder, SalesOrderLine> salesOrderLines;
    // BaseEntity fields
    public static volatile SingularAttribute<SalesOrder, LocalDateTime> createdAt;
    public static volatile SingularAttribute<SalesOrder, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<SalesOrder, EntityStatus> entityStatus;
    public static volatile SingularAttribute<SalesOrder, Long> version;
}
