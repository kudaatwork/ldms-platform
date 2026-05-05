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
import projectlx.co.zw.audittrail.utils.dtos.AuditLogChurnHistoryDto;
import projectlx.co.zw.audittrail.utils.requests.AuditLogChurnHistoryFiltersRequest;

public final class AuditLogChurnHistoryExportResponseFactory {
    private AuditLogChurnHistoryExportResponseFactory() {}

    public static ResponseEntity<byte[]> export(
            AuditLogProcessor auditLogProcessor,
            AuditLogChurnHistoryFiltersRequest filters,
            String format,
            Locale locale,
            String username,
            Logger logger) {
        byte[] data;
        String contentType;
        String filename;
        try {
            List<AuditLogChurnHistoryDto> rows =
                    auditLogProcessor.loadAllMatchingChurnHistoryForExport(filters, locale, username);
            switch (format.toLowerCase()) {
                case "csv":
                    data = auditLogProcessor.exportChurnHistoryToCsv(rows);
                    contentType = "text/csv";
                    filename = "audit-log-churn-history.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = auditLogProcessor.exportChurnHistoryToExcel(rows);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "audit-log-churn-history.xlsx";
                    break;
                case "pdf":
                    data = auditLogProcessor.exportChurnHistoryToPdf(rows);
                    contentType = "application/pdf";
                    filename = "audit-log-churn-history.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export churn history: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
