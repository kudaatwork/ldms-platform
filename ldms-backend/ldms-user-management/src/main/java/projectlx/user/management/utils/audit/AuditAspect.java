package projectlx.user.management.utils.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import projectlx.co.zw.shared_library.utils.audit.AuditClientPlatformSupport;
import projectlx.co.zw.shared_library.utils.audit.AuditHttpTraceSupport;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
import projectlx.user.management.business.logic.api.AuditTrailService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * Auditing must not block the servlet thread: {@code finally} runs before the controller response is flushed.
 * Payload serialization and RabbitMQ publish run on {@link #auditDispatchExecutor}.
 */
@Aspect
@Slf4j
public class AuditAspect {

    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor auditDispatchExecutor;

    @Value("${spring.application.name}")
    private String serviceName;

    public AuditAspect(
            AuditTrailService auditTrailService,
            ObjectMapper objectMapper,
            TaskExecutor auditDispatchExecutor) {
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
        this.auditDispatchExecutor = auditDispatchExecutor;
    }

    @Around("@annotation(projectlx.co.zw.shared_library.utils.audit.Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        Instant requestStart = Instant.now();
        Object result = null;
        String exceptionMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            exceptionMessage = throwable.getMessage();
            throw throwable;
        } finally {
            Instant responseEnd = Instant.now();
            long durationMs = Duration.between(requestStart, responseEnd).toMillis();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Auditable auditable = signature.getMethod().getAnnotation(Auditable.class);

            String username = resolveAuditUsername();

            assert auditable != null;

            Object[] argsCopy = joinPoint.getArgs() != null
                    ? Arrays.copyOf(joinPoint.getArgs(), joinPoint.getArgs().length)
                    : new Object[0];
            String action = auditable.action();
            String traceId = AuditHttpTraceSupport.currentTraceIdFromMdcOrNew();

            final String capturedUsername = username;
            final Object capturedResult = result;
            final String capturedException = exceptionMessage;
            final Instant capturedStart = requestStart;
            final Instant capturedEnd = responseEnd;
            final long capturedDuration = durationMs;
            final String capturedClientPlatform = AuditClientPlatformSupport.fromCurrentRequest();

            try {
                auditDispatchExecutor.execute(() -> {
                    try {
                        String requestPayload;
                        try {
                            requestPayload = objectMapper.writeValueAsString(argsCopy);
                        } catch (Exception ex) {
                            requestPayload = Arrays.toString(argsCopy);
                        }

                        String responsePayload = null;
                        if (capturedResult != null) {
                            try {
                                responsePayload = objectMapper.writeValueAsString(capturedResult);
                            } catch (Exception ex) {
                                responsePayload = capturedResult.toString();
                            }
                        }

                        AuditLogDto logDto = AuditLogDto.builder()
                                .serviceName(serviceName)
                                .traceId(traceId)
                                .timestamp(capturedEnd)
                                .requestTimestamp(capturedStart)
                                .responseTimestamp(capturedEnd)
                                .username(capturedUsername)
                                .clientPlatform(capturedClientPlatform)
                                .action(action)
                                .eventType(AuditEventType.SERVICE_METHOD)
                                .requestPayload(requestPayload)
                                .responsePayload(responsePayload)
                                .responseTimeMs(capturedDuration)
                                .exceptionMessage(capturedException)
                                .build();

                        auditTrailService.sendAuditLog(logDto);
                    } catch (Exception ex) {
                        log.warn("Deferred audit dispatch failed for action {}: {}", action, ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                log.warn("Could not queue audit task for action {}: {}", action, ex.getMessage());
            }
        }
    }

    /**
     * System-surface requests skip JWT and may have no {@link Authentication} in the context yet.
     * A null dereference here would turn a successful controller response into HTTP 500.
     */
    private static String resolveAuditUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "SYSTEM";
        }
        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return "SYSTEM";
        }
        return username;
    }
}
