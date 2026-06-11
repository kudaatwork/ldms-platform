package projectlx.inventory.management.repository;

import org.apache.commons.csv.CSVParser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockTransactionHistoryRepository extends JpaRepository<StockTransactionHistory, Long>,
        JpaSpecificationExecutor<StockTransactionHistory> {
    Optional<StockTransactionHistory> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<StockTransactionHistory> findByEntityStatusNot(EntityStatus entityStatus);
    List<StockTransactionHistory> findByReferenceDocumentIdAndReferenceDocumentTypeAndEntityStatusNot(Long referenceDocumentId,
                                                                                                      ReferenceDocumentType referenceDocumentType,
   EntityStatus entityStatus);

    Optional<StockTransactionHistory> findFirstByReferenceDocumentIdAndReferenceDocumentTypeAndEntityStatusNot(
            Long referenceDocumentId,
            ReferenceDocumentType referenceDocumentType,
            EntityStatus entityStatus);

    List<StockTransactionHistory> findByTimestampBeforeAndEntityStatusNot(LocalDateTime cutoffDate, EntityStatus entityStatus);

    Optional<StockTransactionHistory> findByInventoryItemIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}
