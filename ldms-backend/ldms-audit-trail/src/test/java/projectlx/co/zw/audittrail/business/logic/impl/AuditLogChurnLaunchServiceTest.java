package projectlx.co.zw.audittrail.business.logic.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import projectlx.co.zw.audittrail.service.config.AuditLogChurnBatchConfiguration;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@ExtendWith(MockitoExtension.class)
class AuditLogChurnLaunchServiceTest {

    @Mock
    private Job auditLogChurnJob;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private MessageService messageService;

    @Test
    void launch_submitsJob_whenNoneRunning() throws Exception {
        when(messageService.getMessage(any(), any(), any())).thenReturn("ok");
        JobExecution execution = mock(JobExecution.class);
        when(execution.getId()).thenReturn(42L);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(execution);
        when(jobExplorer.getJobInstances(AuditLogChurnBatchConfiguration.JOB_NAME, 0, 50))
                .thenReturn(Collections.emptyList());

        AuditLogChurnLaunchService service =
                new AuditLogChurnLaunchService(auditLogChurnJob, jobLauncher, jobExplorer, messageService);

        AuditLogResponse response = service.launch(Locale.ENGLISH, "admin@test.local", "MANUAL");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getChurnLaunch()).isNotNull();
        assertThat(response.getChurnLaunch().jobExecutionId()).isEqualTo(42L);
        verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void launch_returnsConflict_whenJobRunning() {
        when(messageService.getMessage(any(), any(), any())).thenReturn("busy");
        JobInstance instance = mock(JobInstance.class);
        JobExecution running = mock(JobExecution.class);
        when(running.isRunning()).thenReturn(true);

        when(jobExplorer.getJobInstances(AuditLogChurnBatchConfiguration.JOB_NAME, 0, 50))
                .thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(running));

        AuditLogChurnLaunchService service =
                new AuditLogChurnLaunchService(auditLogChurnJob, jobLauncher, jobExplorer, messageService);

        AuditLogResponse response = service.launch(Locale.ENGLISH, "admin@test.local", "MANUAL");

        assertThat(response.getStatusCode()).isEqualTo(409);
        assertThat(response.isSuccess()).isFalse();
    }
}
