package projectlx.co.zw.organizationmanagement.utils.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import projectlx.co.zw.organizationmanagement.business.logic.api.AuditTrailService;
import projectlx.co.zw.shared_library.utils.audit.AuditClientPlatformSupport;
import projectlx.co.zw.shared_library.utils.audit.AuditHttpTraceSupport;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

@Aspect
public class AuditAspect {

    private final AuditTrailService auditTrailService;

    public AuditAspect(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

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

            AuditLogDto logDto = AuditLogDto.builder()
                    .serviceName(serviceName)
                    .traceId(AuditHttpTraceSupport.currentTraceIdFromMdcOrNew())
                    .timestamp(responseEnd)
                    .requestTimestamp(requestStart)
                    .responseTimestamp(responseEnd)
                    .username(resolveAuditUsername())
                    .clientPlatform(AuditClientPlatformSupport.fromCurrentRequest())
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
