package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ProductDocument.class)
public class ProductDocument_ {
    public static volatile SingularAttribute<ProductDocument, String> documentId;
    public static volatile SingularAttribute<ProductDocument, String> name;
    public static volatile SingularAttribute<ProductDocument, String> description;
    public static volatile SingularAttribute<ProductDocument, String> entityStatus;
}
