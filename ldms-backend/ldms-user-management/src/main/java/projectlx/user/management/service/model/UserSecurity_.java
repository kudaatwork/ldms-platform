package projectlx.user.management.service.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserSecurity.class)
public class UserSecurity_ {
    public static volatile SingularAttribute<UserSecurity, String> securityQuestion_1;
    public static volatile SingularAttribute<UserSecurity, String> securityAnswer_1;
    public static volatile SingularAttribute<UserSecurity, String> securityQuestion_2;
    public static volatile SingularAttribute<UserSecurity, String> securityAnswer_2;
    public static volatile SingularAttribute<UserSecurity, String> twoFactorAuthSecret;
    public static volatile SingularAttribute<UserSecurity, Boolean> isTwoFactorEnabled;
    public static volatile SingularAttribute<UserSecurity, String> entityStatus;
}
