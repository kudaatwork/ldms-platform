package projectlx.co.zw.audittrail.service.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import projectlx.co.zw.audittrail.utils.config.LdmsAuditProperties;

@Configuration
public class AuditLogChurnBatchLauncherConfiguration {

    @Bean(name = "auditLogAsyncJobLauncher")
    public JobLauncher auditLogAsyncJobLauncher(JobRepository jobRepository, TaskExecutor auditBatchTaskExecutor)
            throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(auditBatchTaskExecutor);
        launcher.afterPropertiesSet();
        return launcher;
    }

    @Bean
    public TaskExecutor auditBatchTaskExecutor(LdmsAuditProperties props) {
        int pool = Math.max(1, props.getChurn().getJobPoolSize());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(pool);
        executor.setMaxPoolSize(pool);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("audit-churn-");
        executor.initialize();
        return executor;
    }
}
