package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CountryDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CountryMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CountryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface CountryServiceProcessor {
    CountryResponse create(CreateCountryRequest request, Locale locale, String username);
    CountryResponse update(EditCountryRequest request, Locale locale, String username);
    CountryResponse findById(Long id, Locale locale, String username);
    CountryResponse findAll(Locale locale, String username);
    CountryResponse delete(Long id, Locale locale, String username);
    CountryResponse findByMultipleFilters(CountryMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<CountryDto> dtoList);
    byte[] exportToExcel(List<CountryDto> dtoList) throws IOException;
    byte[] exportToPdf(List<CountryDto> dtoList) throws DocumentException;
    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}