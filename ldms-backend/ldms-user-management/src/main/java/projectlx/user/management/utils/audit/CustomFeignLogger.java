package projectlx.user.management.utils.audit;

import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.shared_library.utils.audit.AuditHttpTraceSupport;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
import projectlx.user.management.business.logic.api.AuditTrailService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class CustomFeignLogger extends Logger {

    private final AuditTrailService auditTrailService;
    private final String serviceName; // The serviceName will be passed in

    @Override
    protected void log(String configKey, String format, Object... args) {
        // We do our own logging, so this can be a no-op
        log.debug(String.format(methodTag(configKey) + format, args));
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {

        String methodTag = methodTag(configKey);
        Request request = response.request();
        String requestBodyString;

        // NEW LOGIC TO HANDLE MULTIPART FILE UPLOADS
        Map<String, Collection<String>> headers = request.headers();
        if (headers.containsKey("Content-Type") && headers.get("Content-Type").stream().anyMatch(h -> h.contains("multipart/form-data"))) {

            // Try to extract the filename for a more informative log message
            String originalBody = request.body() != null ? new String(request.body(), StandardCharsets.UTF_8) : "";
            Pattern pattern = Pattern.compile("filename=\"(.*?)\"");
            Matcher matcher = pattern.matcher(originalBody);
            String filename = matcher.find() ? matcher.group(1) : "unknown file";

            requestBodyString = String.format("<Multipart File Upload: filename=%s, content not logged>", filename);

        } else {
            // Original logic for non-file uploads
            requestBodyString = request.body() != null ? new String(request.body(), StandardCharsets.UTF_8) : null;
        }

        // The rest of the method remains the same...
        byte[] bodyData = {};
        String responseBody = null;
        if (response.body() != null) {
            bodyData = Util.toByteArray(response.body().asInputStream());
            responseBody = new String(bodyData, StandardCharsets.UTF_8);
        }

        Instant responseEnd = Instant.now();
        Instant requestStart = responseEnd.minusMillis(Math.max(0L, elapsedTime));
        String traceId = AuditHttpTraceSupport.currentTraceIdFromMdcOrNew();
        AuditLogDto logDto = AuditLogDto.builder()
                .serviceName(serviceName)
                .traceId(traceId)
                .timestamp(responseEnd)
                .requestTimestamp(requestStart)
                .responseTimestamp(responseEnd)
                .username("feign-client")
                .action("FEIGN_CALL: " + methodTag)
                .eventType(AuditEventType.FEIGN_CALL)
                .requestUrl(request.url())
                .httpMethod(request.httpMethod().name())
                .httpStatusCode(response.status())
                .requestPayload(requestBodyString) // Use the potentially truncated string
                .responsePayload(responseBody)
                .responseTimeMs(elapsedTime)
                .build();

        auditTrailService.sendAuditLog(logDto);

        return response.toBuilder().body(bodyData).build();
    }
}
