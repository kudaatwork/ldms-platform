package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AdministrativeLevelDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AdministrativeLevelMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AdministrativeLevelResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface AdministrativeLevelServiceProcessor {
    AdministrativeLevelResponse create(CreateAdministrativeLevelRequest request, Locale locale, String username);
    AdministrativeLevelResponse update(EditAdministrativeLevelRequest request, Locale locale, String username);
    AdministrativeLevelResponse findById(Long id, Locale locale, String username);
    AdministrativeLevelResponse findAll(Locale locale, String username);
    AdministrativeLevelResponse delete(Long id, Locale locale, String username);
    AdministrativeLevelResponse findByMultipleFilters(AdministrativeLevelMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<AdministrativeLevelDto> dtoList);
    byte[] exportToExcel(List<AdministrativeLevelDto> dtoList) throws IOException;
    byte[] exportToPdf(List<AdministrativeLevelDto> dtoList) throws DocumentException;
    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}