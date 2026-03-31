package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.GeoCoordinatesDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.GeoCoordinatesMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.GeoCoordinatesResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface GeoCoordinatesServiceProcessor {
    GeoCoordinatesResponse create(CreateGeoCoordinatesRequest request, Locale locale, String username);
    GeoCoordinatesResponse update(EditGeoCoordinatesRequest request, Locale locale, String username);
    GeoCoordinatesResponse findById(Long id, Locale locale, String username);
    GeoCoordinatesResponse findAll(Locale locale, String username);
    GeoCoordinatesResponse delete(Long id, Locale locale, String username);
    GeoCoordinatesResponse findByMultipleFilters(GeoCoordinatesMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<GeoCoordinatesDto> dtoList);
    byte[] exportToExcel(List<GeoCoordinatesDto> dtoList) throws IOException;
    byte[] exportToPdf(List<GeoCoordinatesDto> dtoList) throws DocumentException;
    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}