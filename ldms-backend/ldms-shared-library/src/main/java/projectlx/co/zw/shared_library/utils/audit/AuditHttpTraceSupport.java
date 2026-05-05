package projectlx.co.zw.shared_library.utils.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

/**
 * Resolves inbound trace ids for HTTP audit logs and exposes them on {@link MDC} so
 * {@code @Auditable} aspects and Feign clients can emit the same trace id as the servlet filter.
 */
public final class AuditHttpTraceSupport {

    public static final String MDC_TRACE_ID = "ldms.traceId";

    private AuditHttpTraceSupport() {}

    /**
     * Prefer client-supplied correlation headers, then W3C {@code traceparent}, otherwise a new UUID.
     */
    public static String traceIdFromServletRequest(HttpServletRequest request) {
        if (request == null) {
            return newTraceId();
        }
        String[] names = {"X-Request-Id", "X-Trace-Id", "X-Correlation-Id"};
        for (String name : names) {
            String v = request.getHeader(name);
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        String tp = request.getHeader("traceparent");
        if (StringUtils.hasText(tp)) {
            String[] parts = tp.split("-");
            if (parts.length >= 2 && parts[1].length() == 32) {
                return parts[1];
            }
        }
        return newTraceId();
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString();
    }

    public static void putMdcTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
    }

    public static void clearMdcTraceId() {
        MDC.remove(MDC_TRACE_ID);
    }

    /** Trace id established for the current HTTP request, or a new id if none (e.g. async / Feign). */
    public static String currentTraceIdFromMdcOrNew() {
        String t = MDC.get(MDC_TRACE_ID);
        return StringUtils.hasText(t) ? t : newTraceId();
    }
}
