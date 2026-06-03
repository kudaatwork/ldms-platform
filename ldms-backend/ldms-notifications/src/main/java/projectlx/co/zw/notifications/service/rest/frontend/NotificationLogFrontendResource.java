package projectlx.co.zw.notifications.service.rest.frontend;

import com.lowagie.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.notifications.service.processor.api.NotificationLogProcessor;
import projectlx.co.zw.notifications.utils.requests.NotificationLogMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.responses.NotificationLogResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.export.LdmsExportMediaTypes;

@CrossOrigin
@RestController
@RequestMapping("/ldms-notifications/v1/frontend/notification-log")
@Tag(name = "Notification Log Frontend Resource", description = "Delivery log and queue visibility for the admin portal")
@RequiredArgsConstructor
public class NotificationLogFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(NotificationLogFrontendResource.class);

    private final NotificationLogProcessor notificationLogProcessor;

    @Auditable(action = "FIND_NOTIFICATION_LOG_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.notifications.utils.security.NotificationLogRoles)."
            + "SEARCH_NOTIFICATION_LOGS.toString())")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Find notification delivery log entries", description = "Paged delivery log including QUEUED, PENDING, SENT, FAILED, and SKIPPED rows, plus RabbitMQ queue depth.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log entries retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid filter request"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public NotificationLogResponse findByMultipleFilters(
            @Valid @RequestBody final NotificationLogMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return notificationLogProcessor.findByMultipleFilters(request, username, locale);
    }

    @Auditable(action = "EXPORT_NOTIFICATION_LOG")
    @PreAuthorize("hasRole(T(projectlx.co.zw.notifications.utils.security.NotificationLogRoles)."
            + "EXPORT_NOTIFICATION_LOGS.toString())")
    @PostMapping("/export")
    @Operation(summary = "Export notification delivery log", description = "Exports filtered log rows as CSV, Excel (XLSX), or PDF.")
    public ResponseEntity<byte[]> export(
            @Valid @RequestBody final NotificationLogMultipleFiltersRequest request,
            @RequestParam(defaultValue = "csv") final String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String baseName = "notification-delivery-log";
        HttpHeaders headers = new HttpHeaders();

        try {
            byte[] body;
            String normalized = LdmsExportMediaTypes.normalizeFormat(format);
            if (LdmsExportMediaTypes.isExcel(normalized)) {
                body = notificationLogProcessor.exportToExcel(request, username, locale);
                headers.setContentDispositionFormData("attachment", baseName + ".xlsx");
                headers.setContentType(MediaType.parseMediaType(LdmsExportMediaTypes.XLSX));
            } else if (LdmsExportMediaTypes.isPdf(normalized)) {
                body = notificationLogProcessor.exportToPdf(request, username, locale);
                headers.setContentDispositionFormData("attachment", baseName + ".pdf");
                headers.setContentType(MediaType.parseMediaType(LdmsExportMediaTypes.PDF));
            } else if (LdmsExportMediaTypes.isCsv(normalized)) {
                body = notificationLogProcessor.exportToCsv(request, username, locale);
                headers.setContentDispositionFormData("attachment", baseName + ".csv");
                headers.setContentType(MediaType.parseMediaType(LdmsExportMediaTypes.CSV));
            } else {
                return ResponseEntity.badRequest()
                        .body(("Unsupported export format: " + format).getBytes(StandardCharsets.UTF_8));
            }
            logger.info("Exported notification log as {} ({} bytes) for {}", normalized, body.length, username);
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        } catch (IOException | DocumentException e) {
            logger.error("Notification log export failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "CHURN_OUT_NOTIFICATION_LOG")
    @PreAuthorize("hasRole(T(projectlx.co.zw.notifications.utils.security.NotificationLogRoles)."
            + "EXPORT_NOTIFICATION_LOGS.toString())")
    @PostMapping("/churn-out")
    @Operation(summary = "Churn out notification delivery log", description = "Marks all notification log entries as deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification log churn out completed"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public NotificationLogResponse churnOut(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return notificationLogProcessor.churnOutLogs(username, locale);
    }
}
