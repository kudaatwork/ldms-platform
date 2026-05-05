package projectlx.co.zw.audittrail.business.logic.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import projectlx.co.zw.audittrail.service.config.AuditLogChurnBatchConfiguration;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogChurnLaunchDto;
import projectlx.co.zw.audittrail.utils.enums.I18Code;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Service
public class AuditLogChurnLaunchService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogChurnLaunchService.class);

    private final Job auditLogChurnJob;
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final MessageService messageService;

    public AuditLogChurnLaunchService(
            @Qualifier(AuditLogChurnBatchConfiguration.JOB_NAME) Job auditLogChurnJob,
            @Qualifier("auditLogAsyncJobLauncher") JobLauncher jobLauncher,
            JobExplorer jobExplorer,
            MessageService messageService) {
        this.auditLogChurnJob = auditLogChurnJob;
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.messageService = messageService;
    }

    public AuditLogResponse launch(Locale locale, String username, String triggerType) {
        if (hasRunningChurnJob()) {
            String responseMessage =
                    messageService.getMessage(I18Code.AUDIT_LOG_CHURN_ALREADY_RUNNING.getCode(), new String[] {}, locale);
            return buildWithErrors(
                    409,
                    false,
                    responseMessage,
                    List.of(responseMessage));
        }

        String batchReference = UUID.randomUUID().toString();
        JobParameters parameters = new JobParametersBuilder()
                .addString("triggerType", triggerType != null ? triggerType : "SYSTEM")
                .addString("triggeredBy", username != null ? username : "SYSTEM")
                .addString("batchReference", batchReference)
                .addLong("launchTime", System.currentTimeMillis())
                .toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(auditLogChurnJob, parameters);
            LocalDateTime acceptedAt = LocalDateTime.now();
            String msg =
                    messageService.getMessage(I18Code.AUDIT_LOG_CHURN_LAUNCH_ACCEPTED.getCode(), new String[] {}, locale);

            return build(
                    200,
                    true,
                    msg,
                    new AuditLogChurnLaunchDto(
                            execution.getId(),
                            batchReference,
                            acceptedAt,
                            triggerType != null ? triggerType : "SYSTEM",
                            username != null ? username : "SYSTEM",
                            msg));
        } catch (JobExecutionException ex) {
            logger.error("Failed to launch churn job: {}", ex.getMessage(), ex);
            String responseMessage =
                    messageService.getMessage(I18Code.AUDIT_LOG_CHURN_OUT_FAILED.getCode(), new String[] {}, locale);
            return buildWithErrors(
                    500,
                    false,
                    responseMessage,
                    List.of(ex.getMessage() != null ? ex.getMessage() : "Job launch failed"));
        }
    }

    private boolean hasRunningChurnJob() {
        List<JobInstance> instances =
                jobExplorer.getJobInstances(AuditLogChurnBatchConfiguration.JOB_NAME, 0, 50);
        for (JobInstance instance : instances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
            if (executions == null) {
                continue;
            }
            for (JobExecution execution : executions) {
                if (execution.isRunning()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AuditLogResponse build(int statusCode, boolean success, String message, AuditLogChurnLaunchDto launch) {
        AuditLogResponse response = new AuditLogResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setChurnLaunch(launch);
        return response;
    }

    private static AuditLogResponse buildWithErrors(
            int statusCode, boolean success, String message, List<String> errors) {
        AuditLogResponse response = new AuditLogResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
