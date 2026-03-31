package projectlx.co.zw.notificationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import projectlx.co.zw.notificationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.notificationsmanagementservice.utils.dtos.NotificationTemplateDto;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.CreateTemplateRequest;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.TemplateMultipleFiltersRequest;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.UpdateTemplateRequest;
import projectlx.co.zw.notificationsmanagementservice.utils.responses.TemplateResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface NotificationTemplateService {
    TemplateResponse create(CreateTemplateRequest createTemplateRequest, Locale locale, String username);
    TemplateResponse findById(Long id, Locale locale, String username);
    TemplateResponse findAllAsList(Locale locale, String username);
    TemplateResponse update(UpdateTemplateRequest updateTemplateRequest, String username, Locale locale);
    TemplateResponse delete(Long id, Locale locale, String username);
    TemplateResponse findByMultipleFilters(TemplateMultipleFiltersRequest templateMultipleFiltersRequest,
                                           String username, Locale locale);
    byte[] exportToCsv(List<NotificationTemplateDto> agents);
    byte[] exportToExcel(List<NotificationTemplateDto> agents) throws IOException;
    byte[] exportToPdf(List<NotificationTemplateDto> agents) throws DocumentException;
    ImportSummary importTemplatesFromCsv(InputStream csvInputStream) throws IOException;
}
