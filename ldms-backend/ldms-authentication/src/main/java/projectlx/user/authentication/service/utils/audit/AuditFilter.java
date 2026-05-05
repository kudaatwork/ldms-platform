package projectlx.user.authentication.service.utils.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import projectlx.co.zw.shared_library.utils.audit.AuditHttpTraceSupport;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
import projectlx.user.authentication.service.business.logic.api.AuditTrailService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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
                // ignore
            }

            String requestBody = getBody(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
            String responseBody = getBody(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding());

            try {
                responseWrapper.copyBodyToResponse();
            } catch (IOException ignored) {
                // ignore
            }

            // Don't log for the audit service's own endpoints if it had any
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

                auditTrailService.sendAuditLog(logDto);
            }
            AuditHttpTraceSupport.clearMdcTraceId();
        }
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
