package projectlx.co.zw.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Strips spoofed identity headers and forwards {@code Authorization} unchanged.
 * Microservices validate Bearer tokens locally ({@code JwtAuthenticationFilter}); duplicating
 * role lists into {@code X-User-Roles} can exceed HTTP header size limits for admin users.
 */
@Component
public class JwtValidationGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_GATEWAY_AUTHENTICATED = "X-Gateway-Authenticated";

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        ServerHttpRequest clearedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_ROLES);
                    headers.remove(HEADER_GATEWAY_AUTHENTICATED);
                })
                .build();

        return chain.filter(exchange.mutate().request(clearedRequest).build());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
