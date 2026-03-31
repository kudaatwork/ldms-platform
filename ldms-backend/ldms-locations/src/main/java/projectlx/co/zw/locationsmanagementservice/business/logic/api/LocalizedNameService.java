package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocalizedNameDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocalizedNameMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocalizedNameResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface LocalizedNameService {
    LocalizedNameResponse create(CreateLocalizedNameRequest request, Locale locale, String username);
    LocalizedNameResponse findById(Long id, Locale locale, String username);
    LocalizedNameResponse findAllAsList(Locale locale, String username);
    LocalizedNameResponse update(EditLocalizedNameRequest request, String username, Locale locale);
    LocalizedNameResponse delete(Long id, Locale locale, String username);
    LocalizedNameResponse findByMultipleFilters(LocalizedNameMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<LocalizedNameDto> items);
    byte[] exportToExcel(List<LocalizedNameDto> items) throws IOException;
    byte[] exportToPdf(List<LocalizedNameDto> items) throws DocumentException;
    ImportSummary importLocalizedNameFromCsv(InputStream csvInputStream) throws IOException;
}