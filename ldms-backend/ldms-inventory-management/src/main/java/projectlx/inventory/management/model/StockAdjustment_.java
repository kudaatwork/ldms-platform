package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(StockAdjustment.class)
public class StockAdjustment_ {
    public static volatile SingularAttribute<StockAdjustment, Double> quantityDelta;
    public static volatile SingularAttribute<StockAdjustment, String> reason;
    public static volatile SingularAttribute<StockAdjustment, LocalDateTime> adjustedAt;
    public static volatile SingularAttribute<StockAdjustment, String> entityStatus;
}
