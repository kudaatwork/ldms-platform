package projectlx.user.authentication.service.utils.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import projectlx.co.zw.shared_library.utils.audit.AuditHttpTraceSupport;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
import projectlx.user.authentication.service.business.logic.api.AuditTrailService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditTrailService auditTrailService;

    @Value("${spring.application.name}")
    private String serviceName;

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

            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if (username == null || username.isEmpty() || username.equals("anonymousUser")) {
                username = "SYSTEM"; // Fallback for unauthenticated or anonymous users
            }

            assert auditable != null;
            String traceId = AuditHttpTraceSupport.currentTraceIdFromMdcOrNew();
            AuditLogDto logDto = AuditLogDto.builder()
                    .serviceName(serviceName)
                    .traceId(traceId)
                    .timestamp(responseEnd)
                    .requestTimestamp(requestStart)
                    .responseTimestamp(responseEnd)
                    .username(username)
                    .action(auditable.action())
                    .eventType(AuditEventType.SERVICE_METHOD)
                    .requestPayload(Arrays.toString(joinPoint.getArgs()))
                    .responsePayload(result != null ? result.toString() : null)
                    .responseTimeMs(durationMs)
                    .exceptionMessage(exceptionMessage)
                    .build();

            auditTrailService.sendAuditLog(logDto);
        }
    }
}
