package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.ProductDocument;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface ProductDocumentRepository extends JpaRepository<ProductDocument, Long>, JpaSpecificationExecutor<ProductDocument> {
    Optional<ProductDocument> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<ProductDocument> findByEntityStatusNot(EntityStatus entityStatus);
}
