package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LanguageDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LanguageMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LanguageResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface LanguageServiceProcessor {
    LanguageResponse create(CreateLanguageRequest request, Locale locale, String username);
    LanguageResponse update(EditLanguageRequest request, Locale locale, String username);
    LanguageResponse findById(Long id, Locale locale, String username);
    LanguageResponse findAll(Locale locale, String username);
    LanguageResponse delete(Long id, Locale locale, String username);
    LanguageResponse findByMultipleFilters(LanguageMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<LanguageDto> dtoList);
    byte[] exportToExcel(List<LanguageDto> dtoList) throws IOException;
    byte[] exportToPdf(List<LanguageDto> dtoList) throws DocumentException;
    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}