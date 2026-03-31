package projectlx.user.management.service.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserRole.class)
public class UserRole_ {
    public static volatile SingularAttribute<UserRole, String> role;
    public static volatile SingularAttribute<UserRole, String> description;
    public static volatile SingularAttribute<UserRole, String> entityStatus;
}
