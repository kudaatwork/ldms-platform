package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(LocalizedName.class)
public class LocalizedName_ {
    public static volatile SingularAttribute<LocalizedName, String> value;
    public static volatile SingularAttribute<LocalizedName, Language> language;
    public static volatile SingularAttribute<LocalizedName, EntityStatus> entityStatus;
}