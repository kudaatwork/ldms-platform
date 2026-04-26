package projectlx.user.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UserAccount.class)
public class UserAccount_ {
    public static volatile SingularAttribute<UserAccount, String> phoneNumber;
    public static volatile SingularAttribute<UserAccount, String> accountNumber;
    public static volatile SingularAttribute<UserAccount, Boolean> isAccountLocked;
    public static volatile SingularAttribute<UserAccount, String> entityStatus;
}
