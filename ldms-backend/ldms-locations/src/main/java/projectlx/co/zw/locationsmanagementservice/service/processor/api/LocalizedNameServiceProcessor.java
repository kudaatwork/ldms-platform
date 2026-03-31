package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocalizedNameDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocalizedNameMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocalizedNameResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface LocalizedNameServiceProcessor {
    LocalizedNameResponse create(CreateLocalizedNameRequest request, Locale locale, String username);
    LocalizedNameResponse update(EditLocalizedNameRequest request, Locale locale, String username);
    LocalizedNameResponse findById(Long id, Locale locale, String username);
    LocalizedNameResponse findAll(Locale locale, String username);
    LocalizedNameResponse delete(Long id, Locale locale, String username);
    LocalizedNameResponse findByMultipleFilters(LocalizedNameMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<LocalizedNameDto> dtoList);
    byte[] exportToExcel(List<LocalizedNameDto> dtoList) throws IOException;
    byte[] exportToPdf(List<LocalizedNameDto> dtoList) throws DocumentException;
    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}