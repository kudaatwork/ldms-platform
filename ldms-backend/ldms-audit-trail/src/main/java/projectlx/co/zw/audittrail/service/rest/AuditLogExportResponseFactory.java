package projectlx.co.zw.audittrail.service.rest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import projectlx.co.zw.audittrail.service.processor.api.AuditLogProcessor;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogDto;
import projectlx.co.zw.audittrail.utils.requests.AuditLogMultipleFiltersRequest;

public final class AuditLogExportResponseFactory {

    private AuditLogExportResponseFactory() {}

    public static ResponseEntity<byte[]> export(
            AuditLogProcessor auditLogProcessor,
            AuditLogMultipleFiltersRequest filters,
            String format,
            Locale locale,
            String username,
            Logger logger) {

        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export audit logs in {} format with filters: {}", format, filters);

            List<AuditLogDto> rows = auditLogProcessor.loadAllMatchingForExport(filters, locale, username);

            switch (format.toLowerCase()) {
                case "csv":
                    data = auditLogProcessor.exportToCsv(rows);
                    contentType = "text/csv";
                    filename = "audit-logs.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = auditLogProcessor.exportToExcel(rows);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "audit-logs.xlsx";
                    break;

                case "pdf":
                    data = auditLogProcessor.exportToPdf(rows);
                    contentType = "application/pdf";
                    filename = "audit-logs.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported audit logs in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export audit logs: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
