package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CountryDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CountryMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CountryResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface CountryService {
    CountryResponse create(CreateCountryRequest request, Locale locale, String username);
    CountryResponse findById(Long id, Locale locale, String username);
    CountryResponse findAllAsList(Locale locale, String username);
    CountryResponse update(EditCountryRequest request, String username, Locale locale);
    CountryResponse delete(Long id, Locale locale, String username);
    CountryResponse findByMultipleFilters(CountryMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<CountryDto> items);
    byte[] exportToExcel(List<CountryDto> items) throws IOException;
    byte[] exportToPdf(List<CountryDto> items) throws DocumentException;
    ImportSummary importCountryFromCsv(InputStream csvInputStream) throws IOException;
}