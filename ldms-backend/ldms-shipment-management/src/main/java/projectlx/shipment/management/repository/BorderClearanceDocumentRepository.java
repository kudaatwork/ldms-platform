package projectlx.shipment.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.shipment.management.model.BorderClearanceDocument;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface BorderClearanceDocumentRepository extends JpaRepository<BorderClearanceDocument, Long>,
        JpaSpecificationExecutor<BorderClearanceDocument> {

    Optional<BorderClearanceDocument> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<BorderClearanceDocument> findAllByCaseIdAndEntityStatusNot(Long caseId, EntityStatus entityStatus);
}
