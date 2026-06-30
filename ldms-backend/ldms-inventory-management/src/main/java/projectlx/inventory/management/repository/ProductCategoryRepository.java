package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.inventory.management.model.ProductCategory;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long>, JpaSpecificationExecutor<ProductCategory> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductCategory> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<ProductCategory> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<ProductCategory> findByNameAndSupplierIdAndEntityStatusNot(String name, Long supplierId, EntityStatus entityStatus);
    List<ProductCategory> findByEntityStatusNot(EntityStatus entityStatus);
    List<ProductCategory> findBySupplierIdAndEntityStatusNot(Long supplierId, EntityStatus entityStatus);
}
