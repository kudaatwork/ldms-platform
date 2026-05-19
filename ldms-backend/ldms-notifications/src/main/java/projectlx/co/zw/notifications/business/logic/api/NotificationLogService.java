package projectlx.co.zw.notifications.business.logic.api;

import com.lowagie.text.DocumentException;
import java.io.IOException;
import java.util.Locale;
import projectlx.co.zw.notifications.utils.requests.NotificationLogMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.responses.NotificationLogResponse;

public interface NotificationLogService {

    NotificationLogResponse findByMultipleFilters(NotificationLogMultipleFiltersRequest request, String username, Locale locale);

    byte[] exportToCsv(NotificationLogMultipleFiltersRequest request, String username, Locale locale);

    byte[] exportToExcel(NotificationLogMultipleFiltersRequest request, String username, Locale locale) throws IOException;

    byte[] exportToPdf(NotificationLogMultipleFiltersRequest request, String username, Locale locale) throws DocumentException;
}
