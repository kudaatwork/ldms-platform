package projectlx.co.zw.apigateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Maps downstream connection failures (service not started) to 503 with a actionable message
 * instead of an opaque 500 from the gateway.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayDownstreamErrorWebExceptionHandler implements ErrorWebExceptionHandler, ApplicationContextAware {

    private final ObjectMapper objectMapper;
    private final int authenticationPort;
    private final int locationsPort;
    private final int auditTrailPort;
    private final int fleetManagementPort;
    private ErrorWebExceptionHandler delegate;

    public GatewayDownstreamErrorWebExceptionHandler(
            ObjectMapper objectMapper,
            @Value("${ldms.gateway.authentication.port:8083}") int authenticationPort,
            @Value("${ldms.gateway.locations.port:8084}") int locationsPort,
            @Value("${ldms.gateway.audit-trail.port:8099}") int auditTrailPort,
            @Value("${FLEET_MANAGEMENT_PORT:8089}") int fleetManagementPort) {
        this.objectMapper = objectMapper;
        this.authenticationPort = authenticationPort;
        this.locationsPort = locationsPort;
        this.auditTrailPort = auditTrailPort;
        this.fleetManagementPort = fleetManagementPort;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        delegate = applicationContext.getBeansOfType(ErrorWebExceptionHandler.class).values().stream()
                .filter(handler -> handler != this)
                .filter(handler -> handler.getClass().getName().contains("DefaultErrorWebExceptionHandler")
                        || handler.getClass().getName().contains("AbstractErrorWebExceptionHandler"))
                .findFirst()
                .orElseGet(() -> applicationContext.getBeansOfType(ErrorWebExceptionHandler.class).values().stream()
                        .filter(handler -> handler != this)
                        .findFirst()
                        .orElse(null));
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (isConnectionRefused(ex)) {
            return writeServiceUnavailable(exchange, resolveMessage(exchange.getRequest().getURI().getPath()));
        }
        if (delegate != null) {
            return delegate.handle(exchange, ex);
        }
        return Mono.error(ex);
    }

    private Mono<Void> writeServiceUnavailable(ServerWebExchange exchange, String message) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statusCode", status.value());
        body.put("success", false);
        body.put("message", message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static boolean isConnectionRefused(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String resolveMessage(String path) {
        if (path == null) {
            return "Downstream microservice is not available.";
        }
        if (path.startsWith("/ldms-authentication")) {
            return "Authentication service is not running. Start ldms-authentication on port "
                    + authenticationPort + ", then retry.";
        }
        if (path.startsWith("/ldms-user-management")) {
            return "User management service is not running. Start ldms-user-management on port 8086, then retry.";
        }
        if (path.startsWith("/ldms-organization-management")) {
            return "Organization management service is not running. Start ldms-organization-management on port 8087.";
        }
        if (path.startsWith("/ldms-locations")) {
            return "Locations service is not running. Start ldms-locations on port "
                    + locationsPort + ", then retry.";
        }
        if (path.startsWith("/ldms-notifications")) {
            return "Notifications service is not running. Start ldms-notifications, then retry.";
        }
        if (path.startsWith("/ldms-fleet-management")) {
            return "Fleet management service is not running. Start ldms-fleet-management on port "
                    + fleetManagementPort + ", then retry.";
        }
        if (path.startsWith("/ldms-inventory-management")) {
            return "Inventory management service is not running. Start ldms-inventory-management on port 8013, then retry.";
        }
        if (path.startsWith("/ldms-shipment-management")) {
            return "Shipment management service is not running. Start ldms-shipment-management on port 8015, then retry.";
        }
        if (path.startsWith("/ldms-trip-tracking")) {
            return "Trip tracking service is not running. Start ldms-trip-tracking on port 8016, then retry.";
        }
        if (path.startsWith("/ldms-messaging-inbound")) {
            return "Messaging bot service is not running or needs a restart. Start ldms-messaging-bot on port 8095 with the latest build, then retry.";
        }
        if (path.startsWith("/ldms-audit-trail")) {
            return "Audit trail service is not running. Start ldms-audit-trail on port "
                    + auditTrailPort + ", then retry.";
        }
        return "Downstream microservice is not available. Check that the target service is started.";
    }
}
