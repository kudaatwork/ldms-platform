package projectlx.inventory.management.batch.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class ArchiveOldTransactionsTasklet implements Tasklet {

    private final StockTransactionHistoryRepository transactionHistoryRepository;
    
    @Value("${batch.jobs.data-archiving.days-to-archive:365}")
    private int daysToArchive;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToArchive);
        
        // Find old transactions to archive
        List<StockTransactionHistory> oldTransactions = transactionHistoryRepository
            .findByTimestampBeforeAndEntityStatusNot(cutoffDate, EntityStatus.ARCHIVED);
        
        if (!oldTransactions.isEmpty()) {
            // Mark as archived instead of deleting
            oldTransactions.forEach(t -> t.setEntityStatus(EntityStatus.ARCHIVED));
            transactionHistoryRepository.saveAll(oldTransactions);
            
            log.info("Archived {} old transactions older than {}", oldTransactions.size(), cutoffDate);
            contribution.incrementWriteCount(oldTransactions.size());
        }
        
        return RepeatStatus.FINISHED;
    }
}