package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(City.class)
public class City_ {

    public static volatile SingularAttribute<City, String> name;
    public static volatile SingularAttribute<City, String> code;
    public static volatile SingularAttribute<City, District> district;
    public static volatile SingularAttribute<City, EntityStatus> entityStatus;
}
