package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Language.class)
public class Language_ {
    public static volatile SingularAttribute<Language, String> name;
    public static volatile SingularAttribute<Language, String> isoCode;
    public static volatile SingularAttribute<Language, String> nativeName;
    public static volatile SingularAttribute<Language, EntityStatus> entityStatus;
}