package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AdministrativeLevel.class)
public class AdministrativeLevel_ {
    public static volatile SingularAttribute<AdministrativeLevel, Long> id;
    public static volatile SingularAttribute<AdministrativeLevel, String> name;
    public static volatile SingularAttribute<AdministrativeLevel, String> code;
    public static volatile SingularAttribute<AdministrativeLevel, Integer> level;
    public static volatile SingularAttribute<AdministrativeLevel, String> description;
    public static volatile SingularAttribute<AdministrativeLevel, Country> country;
    public static volatile SingularAttribute<AdministrativeLevel, LocalDateTime> createdAt;
    public static volatile SingularAttribute<AdministrativeLevel, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<AdministrativeLevel, EntityStatus> entityStatus;
}