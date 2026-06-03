package projectlx.co.zw.shared_library.utils.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import projectlx.co.zw.shared_library.utils.constants.Constants;

/**
 * Resolves the originating LDMS client from {@link Constants#LDMS_CLIENT_PLATFORM}.
 */
public final class AuditClientPlatformSupport {

    private AuditClientPlatformSupport() {}

    public static String fromCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return fromHttpRequest(servletAttributes.getRequest());
    }

    public static String fromHttpRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String raw = request.getHeader(Constants.LDMS_CLIENT_PLATFORM);
        return normalize(raw);
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 50) {
            return trimmed.substring(0, 50).toUpperCase(Locale.ROOT);
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
