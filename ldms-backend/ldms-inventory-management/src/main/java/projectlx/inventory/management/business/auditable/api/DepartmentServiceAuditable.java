package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.Department;

import java.util.Locale;

public interface DepartmentServiceAuditable {

    Department create(Department department, Locale locale, String username);

    Department update(Department department, Locale locale, String username);

    Department delete(Department department, Locale locale);
}
