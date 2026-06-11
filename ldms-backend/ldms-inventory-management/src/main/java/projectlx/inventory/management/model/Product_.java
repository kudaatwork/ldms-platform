package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.math.BigDecimal;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Product.class)
public class Product_ {
    public static volatile SingularAttribute<Product, String> name;
    public static volatile SingularAttribute<Product, String> description;
    public static volatile SingularAttribute<Product, BigDecimal> startPrice;
    public static volatile SingularAttribute<Product, BigDecimal> endPrice;
    public static volatile SingularAttribute<Product, UnitOfMeasure> unitOfMeasure;
    public static volatile SingularAttribute<Product, String> productCode;
    public static volatile SingularAttribute<Product, String> barcode;
    public static volatile SingularAttribute<Product, String> manufacturer;
    public static volatile SingularAttribute<Product, Long> imageId;
    public static volatile SingularAttribute<Product, String> entityStatus;
}
