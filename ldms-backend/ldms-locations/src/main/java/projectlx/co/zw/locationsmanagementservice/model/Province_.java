package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Province.class)
public class Province_ {
    public static volatile SingularAttribute<Province, String> name;
    public static volatile SingularAttribute<Province, String> code;
    public static volatile SingularAttribute<Province, Country> country;
    public static volatile SingularAttribute<Province, AdministrativeLevel> administrativeLevel;
    public static volatile SingularAttribute<Province, EntityStatus> entityStatus;
}