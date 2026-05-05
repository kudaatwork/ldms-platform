package projectlx.co.zw.audittrail.business.logic.api;

import com.lowagie.text.DocumentException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogChurnHistoryDto;
import projectlx.co.zw.audittrail.utils.requests.AuditLogChurnHistoryFiltersRequest;
import projectlx.co.zw.audittrail.utils.requests.AuditLogMultipleFiltersRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;

public interface AuditLogService {

    AuditLogResponse findByMultipleFilters(AuditLogMultipleFiltersRequest request, Locale locale, String username);

    AuditLogResponse findById(Long id, Locale locale, String username);

    AuditLogResponse findByTraceId(String traceId, Locale locale, String username);

    AuditLogResponse getServiceStats(String serviceName, int hours, Locale locale, String username);

    AuditLogResponse getChurnOutHistory(int page, int size, Locale locale, String username);

    AuditLogResponse getChurnOutHistory(
            int page,
            int size,
            String triggerType,
            String status,
            String triggeredBy,
            String batchReference,
            LocalDateTime from,
            LocalDateTime to,
            Locale locale,
            String username);

    AuditLogResponse churnOutRequestLogs(Locale locale, String username, String triggerType);

    List<AuditLogChurnHistoryDto> loadAllMatchingChurnHistory(
            AuditLogChurnHistoryFiltersRequest request, Locale locale, String username);

    byte[] exportToCsv(List<AuditLogDto> items);

    byte[] exportToExcel(List<AuditLogDto> items) throws IOException;

    byte[] exportToPdf(List<AuditLogDto> items) throws DocumentException;

    byte[] exportChurnHistoryToCsv(List<AuditLogChurnHistoryDto> items);

    byte[] exportChurnHistoryToExcel(List<AuditLogChurnHistoryDto> items) throws IOException;

    byte[] exportChurnHistoryToPdf(List<AuditLogChurnHistoryDto> items) throws DocumentException;
}
