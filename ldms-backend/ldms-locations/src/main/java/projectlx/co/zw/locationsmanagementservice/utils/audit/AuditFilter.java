package projectlx.co.zw.locationsmanagementservice.utils.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AuditTrailService;
import projectlx.co.zw.shared_library.utils.audit.AuditHttpTraceSupport;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class AuditFilter extends OncePerRequestFilter {

    private final AuditTrailService auditTrailService;

    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Caching wrappers to allow reading request/response body multiple times
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Instant requestStart = Instant.now();
        String traceId = AuditHttpTraceSupport.traceIdFromServletRequest(request);
        AuditHttpTraceSupport.putMdcTraceId(traceId);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            Instant responseEnd = Instant.now();

            try {
                responseWrapper.setHeader("X-Trace-Id", traceId);
            } catch (Exception ignored) {
                // wrapper may not support headers on some paths
            }

            String requestBody = getBody(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
            String responseBody = getBody(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding());

            // Copy the cached response to the client before any slow work (e.g. RabbitMQ audit publish).
            // Otherwise a blocked sendAuditLog prevents copyBodyToResponse and the client sees no body / hang.
            try {
                responseWrapper.copyBodyToResponse();
            } catch (IOException e) {
                log.error("Failed to copy response body: {}", e.getMessage());
            }

            if (!request.getRequestURI().contains("actuator")) {

                String username = "SYSTEM"; // Default fallback

                var authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null) {

                    String authName = authentication.getName();

                    if (authName != null && !authName.isEmpty() && !authName.equals("anonymousUser")) {
                        username = authName;
                    }
                }

                AuditLogDto logDto = AuditLogDto.builder()
                        .serviceName(serviceName)
                        .traceId(traceId)
                        .timestamp(responseEnd)
                        .requestTimestamp(requestStart)
                        .responseTimestamp(responseEnd)
                        .username(username)
                        .clientIpAddress(request.getRemoteAddr())
                        .action("HTTP_REQUEST")
                        .eventType(AuditEventType.WEB_REQUEST)
                        .requestUrl(request.getRequestURI())
                        .httpMethod(request.getMethod())
                        .httpStatusCode(response.getStatus())
                        .requestPayload(requestBody)
                        .responsePayload(responseBody)
                        .responseTimeMs(duration)
                        .requestHeaders(getRequestHeaders(request))
                        .curlCommand(generateCurl(requestWrapper, requestBody))
                        .build();

                scheduleAuditSend(logDto);
            }
            AuditHttpTraceSupport.clearMdcTraceId();
        }
    }

    private void scheduleAuditSend(AuditLogDto logDto) {
        CompletableFuture.runAsync(() -> {
            try {
                auditTrailService.sendAuditLog(logDto);
            } catch (Exception e) {
                log.warn("Audit log failed: {}", e.getMessage());
            }
        });
    }

    private String getBody(byte[] content, String encoding) {

        try {
            return new String(content, encoding);
        } catch (UnsupportedEncodingException e) {
            return "Failed to read body";
        }
    }

    private Map<String, String> getRequestHeaders(HttpServletRequest request) {

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            // MASK SENSITIVE HEADERS
            if (key.equalsIgnoreCase("authorization") || key.equalsIgnoreCase("cookie")) {
                headers.put(key, "******");
            } else {
                headers.put(key, request.getHeader(key));
            }
        }
        return headers;
    }

    private String generateCurl(ContentCachingRequestWrapper request, String body) {

        StringBuilder curl = new StringBuilder("curl -X " + request.getMethod());

        // Add Headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            curl.append(" -H '").append(key).append(": ");
            if (key.equalsIgnoreCase("authorization") || key.equalsIgnoreCase("cookie")) {
                curl.append("******'");
            } else {
                curl.append(request.getHeader(key)).append("'");
            }
        }

        // Add URL
        curl.append(" '").append(request.getRequestURL());
        if (request.getQueryString() != null) {
            curl.append("?").append(request.getQueryString());
        }
        curl.append("'");

        // Add Body
        if (body != null && !body.isEmpty()) {
            curl.append(" -d '").append(body).append("'");
        }

        return curl.toString();
    }
}
