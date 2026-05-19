package projectlx.co.zw.notifications.service.processor.impl;

import com.lowagie.text.DocumentException;
import java.io.IOException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogService;
import projectlx.co.zw.notifications.service.processor.api.NotificationLogProcessor;
import projectlx.co.zw.notifications.utils.requests.NotificationLogMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.responses.NotificationLogResponse;

@RequiredArgsConstructor
public class NotificationLogProcessorImpl implements NotificationLogProcessor {

    private static final Logger logger = LoggerFactory.getLogger(NotificationLogProcessorImpl.class);

    private final NotificationLogService notificationLogService;

    @Override
    public NotificationLogResponse findByMultipleFilters(
            NotificationLogMultipleFiltersRequest request, String username, Locale locale) {

        logger.info("Incoming request to find notification log entries: {}", request);
        NotificationLogResponse response = notificationLogService.findByMultipleFilters(request, username, locale);
        logger.info("Outgoing notification log response. Status Code: {}", response.getStatusCode());
        return response;
    }

    @Override
    public byte[] exportToCsv(NotificationLogMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to export notification log as CSV");
        return notificationLogService.exportToCsv(request, username, locale);
    }

    @Override
    public byte[] exportToExcel(NotificationLogMultipleFiltersRequest request, String username, Locale locale)
            throws IOException {
        logger.info("Incoming request to export notification log as Excel");
        return notificationLogService.exportToExcel(request, username, locale);
    }

    @Override
    public byte[] exportToPdf(NotificationLogMultipleFiltersRequest request, String username, Locale locale)
            throws DocumentException {
        logger.info("Incoming request to export notification log as PDF");
        return notificationLogService.exportToPdf(request, username, locale);
    }
}
