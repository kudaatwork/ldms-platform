package projectlx.user.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserPreferences.class)
public class UserPreferences_ {
    public static volatile SingularAttribute<UserPreferences, String> preferredLanguage;
    public static volatile SingularAttribute<UserPreferences, String> timezone;
    public static volatile SingularAttribute<UserPreferences, String> entityStatus;
}
