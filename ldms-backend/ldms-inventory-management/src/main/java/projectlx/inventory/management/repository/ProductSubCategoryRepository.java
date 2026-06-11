package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.ProductSubCategory;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface ProductSubCategoryRepository extends JpaRepository<ProductSubCategory, Long>, JpaSpecificationExecutor<ProductSubCategory> {
    Optional<ProductSubCategory> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<ProductSubCategory> findByCategory_IdAndNameAndEntityStatusNot(Long categoryId, String name, EntityStatus entityStatus);
    List<ProductSubCategory> findByEntityStatusNot(EntityStatus entityStatus);
}
