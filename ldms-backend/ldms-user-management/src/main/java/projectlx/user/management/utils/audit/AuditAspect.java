package projectlx.user.management.utils.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
import projectlx.user.management.business.logic.api.AuditTrailService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Aspect
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

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

            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if (username == null || username.isEmpty() || username.equals("anonymousUser")) {
                username = "SYSTEM"; // Fallback for unauthenticated or anonymous users
            }

            assert auditable != null;

            String requestPayload;
            try {
                requestPayload = objectMapper.writeValueAsString(joinPoint.getArgs());
            } catch (Exception ex) {
                // Fallback: keep payload readable if serialization fails.
                requestPayload = Arrays.toString(joinPoint.getArgs());
            }

            String responsePayload = null;
            if (result != null) {
                try {
                    responsePayload = objectMapper.writeValueAsString(result);
                } catch (Exception ex) {
                    responsePayload = result.toString();
                }
            }

            AuditLogDto logDto = AuditLogDto.builder()
                    .serviceName(serviceName)
                    .timestamp(Instant.now())
                    .traceId(resolveTraceId())
                    .username(username)
                    .action(auditable.action())
                    .eventType(AuditEventType.SERVICE_METHOD)
                    .requestPayload(requestPayload)
                    .responsePayload(responsePayload)
                    .responseTimeMs(endTime - startTime)
                    .exceptionMessage(exceptionMessage)
                    .build();

            auditTrailService.sendAuditLog(logDto);
        }
    }

    private String resolveTraceId() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String traceId = request.getHeader("X-Trace-Id");
                if (traceId != null && !traceId.isBlank()) {
                    return traceId;
                }
            }
        } catch (Exception ignored) {
            // If request context is unavailable, fall back to a new trace id.
        }
        return UUID.randomUUID().toString();
    }
}
