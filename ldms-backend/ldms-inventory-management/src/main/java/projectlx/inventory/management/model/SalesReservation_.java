package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(SalesReservation.class)
public class SalesReservation_ {
    public static volatile SingularAttribute<SalesReservation, Long> id;
    public static volatile SingularAttribute<SalesReservation, String> reservationNumber;
    public static volatile SingularAttribute<SalesReservation, Long> customerId;
    public static volatile SingularAttribute<SalesReservation, Product> product;
    public static volatile SingularAttribute<SalesReservation, WarehouseLocation> warehouseLocation;
    public static volatile SingularAttribute<SalesReservation, BigDecimal> quantityReserved;
    public static volatile SingularAttribute<SalesReservation, LocalDateTime> reservedUntil;
    public static volatile SingularAttribute<SalesReservation, ReservationStatus> reservationStatus;
    public static volatile SingularAttribute<SalesReservation, Long> createdByUserId;
    public static volatile SingularAttribute<SalesReservation, Long> updatedByUserId;
    public static volatile SingularAttribute<SalesReservation, String> notes;
    public static volatile SingularAttribute<SalesReservation, EntityStatus> entityStatus;
    public static volatile SingularAttribute<SalesReservation, LocalDateTime> createdAt;
    public static volatile SingularAttribute<SalesReservation, LocalDateTime> updatedAt;
}
