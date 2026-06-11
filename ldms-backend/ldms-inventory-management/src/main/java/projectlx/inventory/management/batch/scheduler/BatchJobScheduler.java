package projectlx.inventory.management.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
@Slf4j
public class BatchJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job expiredReservationsCleanupJob;
    private final Job lowStockAlertJob;
    private final Job inventoryReconciliationJob;
    private final Job dataArchivingJob;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void runExpiredReservationsCleanup() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(expiredReservationsCleanupJob, params);
            log.info("Expired reservations cleanup job completed");
        } catch (Exception e) {
            log.error("Error running expired reservations cleanup", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?") // Daily at 8 AM
    public void runLowStockAlert() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(lowStockAlertJob, params);
            log.info("Low stock alert job completed");
        } catch (Exception e) {
            log.error("Error running low stock alert job", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void runInventoryReconciliation() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(inventoryReconciliationJob, params);
            log.info("Inventory reconciliation job completed");
        } catch (Exception e) {
            log.error("Error running inventory reconciliation job", e);
        }
    }

    @Scheduled(cron = "0 0 1 1 * ?") // Monthly on 1st at 1 AM
    public void runDataArchiving() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(dataArchivingJob, params);
            log.info("Data archiving job completed");
        } catch (Exception e) {
            log.error("Error running data archiving job", e);
        }
    }
}