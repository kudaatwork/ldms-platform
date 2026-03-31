package projectlx.user.management.service.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Address.class)
public class Address_ {
    public static volatile SingularAttribute<Address, String> streetAddress;
    public static volatile SingularAttribute<Address, String> city;
    public static volatile SingularAttribute<Address, String> state;
    public static volatile SingularAttribute<Address, String> postalCode;
    public static volatile SingularAttribute<Address, String> country;
    public static volatile SingularAttribute<Address, String> entityStatus;
}
