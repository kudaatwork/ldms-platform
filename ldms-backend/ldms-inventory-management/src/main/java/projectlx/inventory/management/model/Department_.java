package projectlx.inventory.management.model;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Department.class)
public class Department_ {
    public static volatile SingularAttribute<Department, String> name;
    public static volatile SingularAttribute<Department, String> departmentCode;
    public static volatile SingularAttribute<Department, String> description;
    public static volatile SingularAttribute<Department, Long> supplierId;
    public static volatile SingularAttribute<Department, String> entityStatus;
}
