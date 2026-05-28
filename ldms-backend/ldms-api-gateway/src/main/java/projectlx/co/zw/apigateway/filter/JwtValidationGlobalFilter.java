package projectlx.co.zw.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import projectlx.co.zw.apigateway.config.GatewayJwtProperties;
import projectlx.co.zw.apigateway.security.GatewayJwtSigningKeys;
import reactor.core.publisher.Mono;

/**
 * Routes all non-public traffic through the gateway and optionally enriches requests with
 * {@code X-User-Id} / {@code X-User-Roles} when the Bearer token validates with {@code gateway.jwt.secret}.
 * <p>
 * JWT enforcement happens on each microservice ({@code SharedJwtSecurityConfig}); the gateway must not
 * reject tokens that were issued by ldms-authentication but signed with a different configured secret
 * (common in local dev when Config Server overrides {@code gateway.jwt.secret}).
 */
@Component
public class JwtValidationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationGlobalFilter.class);

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_GATEWAY_AUTHENTICATED = "X-Gateway-Authenticated";

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private final GatewayJwtProperties jwtProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtValidationGlobalFilter(GatewayJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        ServerHttpRequest clearedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_ROLES);
                    headers.remove(HEADER_GATEWAY_AUTHENTICATED);
                })
                .build();
        ServerWebExchange clearedExchange = exchange.mutate().request(clearedRequest).build();

        if (isPublicPath(path)) {
            return chain.filter(clearedExchange);
        }

        String auth = clearedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth) || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return chain.filter(clearedExchange);
        }

        String token = auth.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return chain.filter(clearedExchange);
        }

        try {
            SecretKey key = GatewayJwtSigningKeys.hmacSha256Key(jwtProperties.getSecret());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            if (!StringUtils.hasText(userId)) {
                log.debug("JWT has no subject; forwarding Authorization to downstream for {}", path);
                return chain.filter(clearedExchange);
            }

            String roles = formatRoles(claims.get(jwtProperties.getRolesClaim()));

            ServerHttpRequest mutated = clearedExchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.set(HEADER_USER_ID, userId.trim());
                        headers.set(HEADER_USER_ROLES, roles);
                        headers.set(HEADER_GATEWAY_AUTHENTICATED, "true");
                        headers.set(HttpHeaders.AUTHORIZATION, auth);
                    })
                    .build();

            return chain.filter(clearedExchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException | IllegalStateException ex) {
            log.debug(
                    "Gateway JWT parse skipped for {} ({}); forwarding Bearer to microservice for validation",
                    path,
                    ex.getMessage());
            return chain.filter(clearedExchange);
        }
    }

    private boolean isPublicPath(String path) {
        for (String pattern : jwtProperties.getPublicPaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static String formatRoles(Object claim) {
        if (claim == null) {
            return "";
        }
        if (claim instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return claim.toString();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
