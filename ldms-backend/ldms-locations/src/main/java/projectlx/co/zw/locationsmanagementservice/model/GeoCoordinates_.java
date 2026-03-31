package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(GeoCoordinates.class)
public class GeoCoordinates_ {
    public static volatile SingularAttribute<GeoCoordinates, BigDecimal> latitude;
    public static volatile SingularAttribute<GeoCoordinates, BigDecimal> longitude;
    public static volatile SingularAttribute<GeoCoordinates, EntityStatus> entityStatus;
}