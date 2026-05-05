package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Village.class)
public class Village_ {

    public static volatile SingularAttribute<Village, String> name;
    public static volatile SingularAttribute<Village, String> code;
    public static volatile SingularAttribute<Village, City> city;
    public static volatile SingularAttribute<Village, District> district;
    public static volatile SingularAttribute<Village, EntityStatus> entityStatus;
}
