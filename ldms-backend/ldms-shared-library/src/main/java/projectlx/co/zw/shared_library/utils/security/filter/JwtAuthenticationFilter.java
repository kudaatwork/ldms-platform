package projectlx.co.zw.shared_library.utils.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;

/**
 * Authenticates LDMS API requests using:
 * <ol>
 *   <li>{@code X-User-Id} (+ optional {@code X-User-Roles}) set by the API gateway after JWT validation</li>
 *   <li>{@code Authorization: Bearer} validated locally with {@code jwt.secret}</li>
 * </ol>
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_GATEWAY_AUTHENTICATED = "X-Gateway-Authenticated";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromGatewayHeaders(request);
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromBearer(request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trust identity headers injected by the API gateway (it strips inbound spoof headers first).
     */
    private void authenticateFromGatewayHeaders(HttpServletRequest request) {
        if (!isGatewayAuthenticated(request)) {
            return;
        }
        String userId = request.getHeader(HEADER_USER_ID);
        if (!StringUtils.hasText(userId)) {
            return;
        }
        List<GrantedAuthority> authorities = toAuthorities(parseGatewayRoles(request.getHeader(HEADER_USER_ROLES)));
        setAuthentication(request, userId.trim(), authorities);
    }

    private static boolean isGatewayAuthenticated(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader(HEADER_GATEWAY_AUTHENTICATED));
    }

    private void authenticateFromBearer(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return;
        }
        String jwt = authHeader.substring(7).trim();
        if (!StringUtils.hasText(jwt)) {
            return;
        }
        try {
            String username = jwtService.extractUsername(jwt);
            if (!StringUtils.hasText(username)) {
                return;
            }
            List<String> roles = jwtService.extractRoles(jwt);
            setAuthentication(request, username.trim(), toAuthorities(roles));
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(HttpServletRequest request, String principal, List<GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private static List<String> parseGatewayRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return Collections.emptyList();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private static List<GrantedAuthority> toAuthorities(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            if (!StringUtils.hasText(role)) {
                continue;
            }
            String normalized = role.trim();
            if (!normalized.startsWith("ROLE_")) {
                normalized = "ROLE_" + normalized;
            }
            authorities.add(new SimpleGrantedAuthority(normalized));
        }
        return authorities;
    }

    private boolean isPublicPath(String path) {
        // Do not treat backoffice as public here: HttpSecurity may permitAll on that surface, but
        // @PreAuthorize("isAuthenticated()") (e.g. GET …/user/me) still needs Bearer / gateway headers parsed.
        return path.startsWith("/actuator")
                || path.contains("/v1/system/")
                || path.contains("/v1/auth/")
                || path.endsWith("/v1/frontend/organization/register")
                || path.contains("/organization/onboarding-status")
                || path.contains("/platform-wallet/public/")
                || path.contains("/bot-session/guest/")
                || path.contains("/help-support/demo-requisition/submit");
    }
}
