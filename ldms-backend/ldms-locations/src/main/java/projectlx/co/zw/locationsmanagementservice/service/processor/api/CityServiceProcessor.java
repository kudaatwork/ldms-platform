package projectlx.co.zw.locationsmanagementservice.service.processor.api;

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

public interface CityServiceProcessor {

    CityResponse create(CreateCityRequest request, Locale locale, String username);

    CityResponse update(EditCityRequest request, Locale locale, String username);

    CityResponse findById(Long id, Locale locale, String username);

    CityResponse findAll(Locale locale, String username);

    CityResponse delete(Long id, Locale locale, String username);

    CityResponse findByMultipleFilters(CityMultipleFiltersRequest request, String username, Locale locale);

    byte[] exportToCsv(List<CityDto> dtoList);

    byte[] exportToExcel(List<CityDto> dtoList) throws IOException;

    byte[] exportToPdf(List<CityDto> dtoList) throws DocumentException;

    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}
