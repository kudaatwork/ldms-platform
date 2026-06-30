package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.inventory.management.business.auditable.api.DepartmentServiceAuditable;
import projectlx.inventory.management.model.Department;
import projectlx.inventory.management.repository.DepartmentRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class DepartmentServiceAuditableImpl implements DepartmentServiceAuditable {

    private final DepartmentRepository departmentRepository;

    @Override
    public Department create(Department department, Locale locale, String username) {
        department.setCreatedBy(username);
        department.setModifiedBy(username);
        return departmentRepository.save(department);
    }

    @Override
    public Department update(Department department, Locale locale, String username) {
        department.setModifiedBy(username);
        return departmentRepository.save(department);
    }

    @Override
    public Department delete(Department department, Locale locale) {
        return departmentRepository.save(department);
    }
}
