package projectlx.billing.payments.business.logic.support;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import projectlx.billing.payments.clients.UserManagementServiceClient;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallerOrganizationResolver {

    private final UserManagementServiceClient userManagementServiceClient;
    private final JwtService jwtService;

    public Long resolveCallerOrganizationId(String username) {
        Long fromJwt = resolveOrganizationIdFromBearerToken();
        if (fromJwt != null && fromJwt > 0) {
            return fromJwt;
        }

        if (!StringUtils.hasText(username)) {
            return null;
        }
        String principal = username.trim();
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(principal);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getOrganizationId() != null
                    && userResponse.getUserDto().getOrganizationId() > 0) {
                return userResponse.getUserDto().getOrganizationId();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve organisation id for user {} via user-management: {}", principal, ex.getMessage());
        }
        return null;
    }

    public Long requireCallerOrganizationId(String username) {
        Long organizationId = resolveCallerOrganizationId(username);
        if (organizationId == null || organizationId < 1) {
            return null;
        }
        return organizationId;
    }

    private Long resolveOrganizationIdFromBearerToken() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
                return null;
            }
            HttpServletRequest request = servletAttrs.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return null;
            }
            String jwt = authHeader.substring(7).trim();
            if (!StringUtils.hasText(jwt)) {
                return null;
            }
            return jwtService.extractOrganizationId(jwt);
        } catch (RuntimeException ex) {
            log.debug("Could not read organizationId from JWT: {}", ex.getMessage());
            return null;
        }
    }
}
