package projectlx.co.zw.notifications.service.processor.impl;

import com.lowagie.text.DocumentException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogService;
import projectlx.co.zw.notifications.service.processor.api.NotificationLogProcessor;
import projectlx.co.zw.notifications.utils.requests.NotificationLogMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.responses.NotificationLogResponse;

public class NotificationLogProcessorImpl implements NotificationLogProcessor {

    private static final Logger logger = LoggerFactory.getLogger(NotificationLogProcessorImpl.class);

    private final NotificationLogService notificationLogService;

    public NotificationLogProcessorImpl(NotificationLogService notificationLogService) {
        this.notificationLogService = notificationLogService;
    }

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

    @Override
    public NotificationLogResponse churnOutLogs(String username, Locale locale) {
        logger.info("Incoming request to churn out notification logs");
        return notificationLogService.churnOutLogs(username, locale);
    }

    @Override
    public int churnOutLogsBefore(LocalDateTime cutoff, String username) {
        logger.info("Incoming scheduled request to churn out notification logs before {}", cutoff);
        return notificationLogService.churnOutLogsBefore(cutoff, username);
    }
}
