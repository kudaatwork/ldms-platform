package projectlx.co.zw.notifications.utils.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;

public final class AuthenticatedUserIdSupport {

    private AuthenticatedUserIdSupport() {
    }

    public static Long resolveUserId(JwtService jwtService) {
        HttpServletRequest request = currentRequest();
        if (request != null && jwtService != null) {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String jwt = authHeader.substring(7).trim();
                if (StringUtils.hasText(jwt)) {
                    try {
                        Long userId = jwtService.extractUserId(jwt);
                        if (userId != null && userId > 0) {
                            return userId;
                        }
                    } catch (RuntimeException ignored) {
                        // fall through
                    }
                }
            }
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            try {
                long parsed = Long.parseLong(authentication.getName().trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // principal is username, not numeric id
            }
        }
        return null;
    }

    private static HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
