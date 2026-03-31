package projectlx.co.zw.notifications.utils.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import projectlx.co.zw.notifications.business.logic.api.AuditTrailService;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
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

        long startTime = System.currentTimeMillis();
        Object result = null;
        String exceptionMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            exceptionMessage = throwable.getMessage();
            throw throwable;
        } finally {
            long endTime = System.currentTimeMillis();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Auditable auditable = signature.getMethod().getAnnotation(Auditable.class);

            // --- THE FIX IS HERE ---
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username;

            // Safely check if a user is authenticated. If not, fall back to a default value.
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                username = authentication.getName();
            } else {
                username = "SYSTEM"; // Fallback for unauthenticated or anonymous calls (e.g., system processes)
            }
            // --- END OF FIX ---


            AuditLogDto logDto = AuditLogDto.builder()
                    .serviceName(serviceName)
                    .timestamp(Instant.now())
                    .username(username)
                    .action(auditable.action())
                    .eventType(AuditEventType.SERVICE_METHOD)
                    .requestPayload(Arrays.toString(joinPoint.getArgs()))
                    .responsePayload(result != null ? result.toString() : null)
                    .responseTimeMs(endTime - startTime)
                    .exceptionMessage(exceptionMessage)
                    .build();

            auditTrailService.sendAuditLog(logDto);
        }
    }
}
