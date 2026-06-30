package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ProductCategory.class)
public class ProductCategory_ {
    public static volatile SingularAttribute<ProductCategory, String> name;
    public static volatile SingularAttribute<ProductCategory, String> description;
    public static volatile SingularAttribute<ProductCategory, Long> supplierId;
    public static volatile SingularAttribute<ProductCategory, String> entityStatus;
}
