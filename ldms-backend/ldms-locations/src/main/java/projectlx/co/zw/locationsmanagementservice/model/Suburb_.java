package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Suburb.class)
public class Suburb_ {
    public static volatile SingularAttribute<Suburb, String> name;
    public static volatile SingularAttribute<Suburb, String> code;
    public static volatile SingularAttribute<Suburb, District> district;
    public static volatile SingularAttribute<Suburb, String> postalCode;
    public static volatile SingularAttribute<Suburb, AdministrativeLevel> administrativeLevel;
    public static volatile SingularAttribute<Suburb, EntityStatus> entityStatus;
}