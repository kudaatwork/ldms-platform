package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CityDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CityMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CityResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface CityService {

    CityResponse create(CreateCityRequest request, Locale locale, String username);

    CityResponse findById(Long id, Locale locale, String username);

    CityResponse findAllAsList(Locale locale, String username);

    CityResponse update(EditCityRequest request, String username, Locale locale);

    CityResponse delete(Long id, Locale locale, String username);

    CityResponse findByMultipleFilters(CityMultipleFiltersRequest request, String username, Locale locale);

    byte[] exportToCsv(List<CityDto> items);

    byte[] exportToExcel(List<CityDto> items) throws IOException;

    byte[] exportToPdf(List<CityDto> items) throws DocumentException;

    ImportSummary importCityFromCsv(InputStream csvInputStream) throws IOException;
}
