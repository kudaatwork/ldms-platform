package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Country.class)
public class Country_ {
    public static volatile SingularAttribute<Country, Long> id;
    public static volatile SingularAttribute<Country, String> name;
    public static volatile SingularAttribute<Country, String> isoAlpha2Code;
    public static volatile SingularAttribute<Country, String> isoAlpha3Code;
    public static volatile SingularAttribute<Country, String> dialCode;
    public static volatile SingularAttribute<Country, String> timezone;
    public static volatile SingularAttribute<Country, String> currencyCode;
    public static volatile SingularAttribute<Country, EntityStatus> entityStatus;
}