package projectlx.user.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(User.class)
public class User_ {
    public static volatile SingularAttribute<User, String> firstName;
    public static volatile SingularAttribute<User, String> lastName;
    public static volatile SingularAttribute<User, String> username;
    public static volatile SingularAttribute<User, String> email;
    public static volatile SingularAttribute<User, String> gender;
    public static volatile SingularAttribute<User, String> phoneNumber;
    public static volatile SingularAttribute<User, String> nationalIdNumber;
    public static volatile SingularAttribute<User, String> passportNumber;
    public static volatile SingularAttribute<User, String> entityStatus;
}
