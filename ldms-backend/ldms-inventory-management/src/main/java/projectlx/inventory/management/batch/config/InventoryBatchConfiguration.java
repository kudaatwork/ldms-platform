package projectlx.inventory.management.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import projectlx.inventory.management.model.InventoryDiscrepancy;
import projectlx.inventory.management.model.LowStockAlert;
import jakarta.persistence.EntityManagerFactory;
import projectlx.inventory.management.batch.processors.ArchiveOldTransactionsTasklet;
import projectlx.inventory.management.batch.processors.ExpiredReservationProcessor;
import projectlx.inventory.management.batch.processors.InventoryReconciliationProcessor;
import projectlx.inventory.management.batch.processors.LowStockAlertProcessor;
import projectlx.inventory.management.batch.service.NotificationService;
import projectlx.inventory.management.batch.writers.ExpiredReservationWriter;
import projectlx.inventory.management.batch.writers.InventoryDiscrepancyWriter;
import projectlx.inventory.management.batch.writers.LowStockAlertWriter;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.inventory.management.repository.InventoryDiscrepancyRepository;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.SalesReservationRepository;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import java.math.BigDecimal;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class InventoryBatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    // Services and repositories
    private final SalesReservationRepository salesReservationRepository;
    private final InventoryItemService inventoryItemService;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockTransactionHistoryRepository transactionHistoryRepository;
    private final InventoryDiscrepancyRepository discrepancyRepository;
    private NotificationService notificationService;

    @Value("${batch.reconciliation.variance-threshold:0.01}")
    private BigDecimal varianceThreshold;

    @Value("${batch.jobs.data-archiving.days-to-archive:365}")
    private int daysToArchive;

    // =============================================================================
    // SERVICE BEANS
    // =============================================================================

    @Bean
    public NotificationService notificationService() {
        return new NotificationService();
    }

    // =============================================================================
    // PROCESSORS BEANS
    // =============================================================================

    @Bean
    public ExpiredReservationProcessor expiredReservationProcessor() {
        return new ExpiredReservationProcessor();
    }

    @Bean
    public LowStockAlertProcessor lowStockAlertProcessor() {
        return new LowStockAlertProcessor();
    }

    @Bean
    public InventoryReconciliationProcessor inventoryReconciliationProcessor() {
        return new InventoryReconciliationProcessor(transactionHistoryRepository, varianceThreshold);
    }

    // =============================================================================
    // WRITERS BEANS
    // =============================================================================

    @Bean
    public ExpiredReservationWriter expiredReservationWriter() {
        return new ExpiredReservationWriter(salesReservationRepository, inventoryItemRepository);
    }

    @Bean
    public LowStockAlertWriter lowStockAlertWriter() {
        return new LowStockAlertWriter(notificationService());
    }

    @Bean
    public InventoryDiscrepancyWriter inventoryDiscrepancyWriter() {
        return new InventoryDiscrepancyWriter(discrepancyRepository, notificationService());
    }

    // =============================================================================
    // TASKLETS BEANS
    // =============================================================================

    @Bean
    public ArchiveOldTransactionsTasklet archiveOldTransactionsTasklet() {
        return new ArchiveOldTransactionsTasklet(transactionHistoryRepository);
    }

    // =============================================================================
    // EXPIRED RESERVATIONS CLEANUP JOB
    // =============================================================================

    @Bean
    public Job expiredReservationsCleanupJob() {
        return new JobBuilder("expiredReservationsCleanupJob", jobRepository)
                .start(expiredReservationsStep())
                .build();
    }

    @Bean
    public Step expiredReservationsStep() {
        return new StepBuilder("expiredReservationsStep", jobRepository)
                .<SalesReservation, SalesReservation>chunk(100, transactionManager)
                .reader(expiredReservationsReader())
                .processor(expiredReservationProcessor())
                .writer(expiredReservationWriter())
                .build();
    }

    @Bean
    public ItemReader<SalesReservation> expiredReservationsReader() {
        return new JpaPagingItemReaderBuilder<SalesReservation>()
                .name("expiredReservationsReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT sr FROM SalesReservation sr WHERE sr.reservedUntil < CURRENT_TIMESTAMP AND sr.reservationStatus = :status")
                .parameterValues(java.util.Map.of("status", ReservationStatus.ACTIVE))
                .pageSize(100)
                .build();
    }

    // =============================================================================
    // LOW STOCK ALERT JOB
    // =============================================================================

    @Bean
    public Job lowStockAlertJob() {
        return new JobBuilder("lowStockAlertJob", jobRepository)
                .start(lowStockAlertStep())
                .build();
    }

    @Bean
    public Step lowStockAlertStep() {
        return new StepBuilder("lowStockAlertStep", jobRepository)
                .<InventoryItem, LowStockAlert>chunk(50, transactionManager)
                .reader(lowStockItemsReader())
                .processor(lowStockAlertProcessor())
                .writer(lowStockAlertWriter())
                .build();
    }

    @Bean
    public ItemReader<InventoryItem> lowStockItemsReader() {
        return new JpaPagingItemReaderBuilder<InventoryItem>()
                .name("lowStockItemsReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ii FROM InventoryItem ii WHERE ii.currentStock <= ii.minStockLevel AND ii.entityStatus != :deleted")
                .parameterValues(java.util.Map.of("deleted", projectlx.co.zw.shared_library.utils.enums.EntityStatus.DELETED))
                .pageSize(50)
                .build();
    }

    // =============================================================================
    // INVENTORY RECONCILIATION JOB
    // =============================================================================

    @Bean
    public Job inventoryReconciliationJob() {
        return new JobBuilder("inventoryReconciliationJob", jobRepository)
                .start(reconciliationStep())
                .build();
    }

    @Bean
    public Step reconciliationStep() {
        return new StepBuilder("reconciliationStep", jobRepository)
                .<InventoryItem, InventoryDiscrepancy>chunk(25, transactionManager)
                .reader(inventoryItemsReader())
                .processor(inventoryReconciliationProcessor())
                .writer(inventoryDiscrepancyWriter())
                .build();
    }

    @Bean
    public ItemReader<InventoryItem> inventoryItemsReader() {
        return new JpaPagingItemReaderBuilder<InventoryItem>()
                .name("inventoryItemsReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ii FROM InventoryItem ii WHERE ii.entityStatus != :deleted")
                .parameterValues(java.util.Map.of("deleted", projectlx.co.zw.shared_library.utils.enums.EntityStatus.DELETED))
                .pageSize(25)
                .build();
    }

    // =============================================================================
    // DATA ARCHIVING JOB
    // =============================================================================

    @Bean
    public Job dataArchivingJob() {
        return new JobBuilder("dataArchivingJob", jobRepository)
                .start(archiveOldTransactionsStep())
                .build();
    }

    @Bean
    public Step archiveOldTransactionsStep() {
        return new StepBuilder("archiveOldTransactionsStep", jobRepository)
                .tasklet(archiveOldTransactionsTasklet(), transactionManager)
                .build();
    }
}