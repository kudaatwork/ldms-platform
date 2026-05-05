package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;
import java.time.LocalDateTime;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Address.class)
public class Address_ {
    public static volatile SingularAttribute<Address, String> line1;
    public static volatile SingularAttribute<Address, String> line2;
    public static volatile SingularAttribute<Address, String> postalCode;
    public static volatile SingularAttribute<Address, SettlementType> settlementType;
    public static volatile SingularAttribute<Address, String> externalSource;
    public static volatile SingularAttribute<Address, String> externalPlaceId;
    public static volatile SingularAttribute<Address, String> formattedAddress;
    public static volatile SingularAttribute<Address, EntityStatus> entityStatus;
}