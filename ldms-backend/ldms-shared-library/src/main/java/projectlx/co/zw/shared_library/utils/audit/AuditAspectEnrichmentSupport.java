package projectlx.co.zw.shared_library.utils.audit;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Enriches audit rows emitted from {@code @Auditable} aspects (e.g. login username before security context is set).
 */
public final class AuditAspectEnrichmentSupport {

    private AuditAspectEnrichmentSupport() {}

    public static String resolveUsername(ProceedingJoinPoint joinPoint, String auditableAction, String securityUsername) {
        if ("USER_AUTHENTICATION".equals(auditableAction) && joinPoint.getArgs().length > 0) {
            String fromRequest = extractUsernameField(joinPoint.getArgs()[0]);
            if (fromRequest != null) {
                return fromRequest;
            }
        }
        return securityUsername;
    }

    private static String extractUsernameField(Object requestBody) {
        if (requestBody == null) {
            return null;
        }
        try {
            var method = requestBody.getClass().getMethod("getUsername");
            Object value = method.invoke(requestBody);
            if (value instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        } catch (ReflectiveOperationException ignored) {
            // not a login request DTO
        }
        return null;
    }
}
