package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.inventory.management.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    Optional<Department> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<Department> findByEntityStatusNot(EntityStatus entityStatus);

    List<Department> findBySupplierIdAndEntityStatusNot(Long supplierId, EntityStatus entityStatus);

    Optional<Department> findByNameAndSupplierIdAndEntityStatusNot(String name, Long supplierId, EntityStatus entityStatus);

    Optional<Department> findByDepartmentCodeAndSupplierIdAndEntityStatusNot(
            String departmentCode, Long supplierId, EntityStatus entityStatus);
}
