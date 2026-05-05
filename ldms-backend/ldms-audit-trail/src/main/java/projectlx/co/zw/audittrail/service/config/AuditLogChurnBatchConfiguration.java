package projectlx.co.zw.audittrail.service.config;

import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import projectlx.co.zw.audittrail.service.batch.AuditLogChunkDeleteWriter;
import projectlx.co.zw.audittrail.service.batch.AuditLogChurnFinalizeTasklet;
import projectlx.co.zw.audittrail.service.batch.AuditLogChurnSnapshotTasklet;
import projectlx.co.zw.audittrail.utils.config.LdmsAuditProperties;

@Configuration
public class AuditLogChurnBatchConfiguration {

    public static final String JOB_NAME = "auditLogChurnJob";

    private final LdmsAuditProperties ldmsAuditProperties;

    public AuditLogChurnBatchConfiguration(LdmsAuditProperties ldmsAuditProperties) {
        this.ldmsAuditProperties = ldmsAuditProperties;
    }

    @Bean(name = JOB_NAME)
    public Job auditLogChurnJob(
            JobRepository jobRepository,
            JobExecutionListener auditLogChurnJobExecutionListener,
            Step churnSnapshotStep,
            Step churnDeleteAuditLogsStep,
            Step churnFinalizeStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(auditLogChurnJobExecutionListener)
                .start(churnSnapshotStep)
                .next(churnDeleteAuditLogsStep)
                .next(churnFinalizeStep)
                .build();
    }

    @Bean
    public Step churnSnapshotStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            AuditLogChurnSnapshotTasklet auditLogChurnSnapshotTasklet) {
        return new StepBuilder("churnSnapshotStep", jobRepository)
                .tasklet(auditLogChurnSnapshotTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step churnDeleteAuditLogsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcCursorItemReader<Long> auditLogIdReader,
            AuditLogChunkDeleteWriter auditLogChunkDeleteWriter) {
        int chunk = Math.max(1, ldmsAuditProperties.getChurn().getChunkSize());
        return new StepBuilder(AuditLogChurnFinalizeTasklet.DELETE_STEP_NAME, jobRepository)
                .<Long, Long>chunk(chunk, transactionManager)
                .reader(auditLogIdReader)
                .writer(auditLogChunkDeleteWriter)
                .build();
    }

    @Bean
    public Step churnFinalizeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            AuditLogChurnFinalizeTasklet auditLogChurnFinalizeTasklet) {
        return new StepBuilder("churnFinalizeStep", jobRepository)
                .tasklet(auditLogChurnFinalizeTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<Long> auditLogIdReader(DataSource dataSource) {
        int fetch = Math.max(1, ldmsAuditProperties.getChurn().getChunkSize());
        JdbcCursorItemReader<Long> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id FROM audit_log ORDER BY id");
        reader.setFetchSize(fetch);
        reader.setRowMapper((rs, rowNum) -> rs.getLong(1));
        return reader;
    }
}
