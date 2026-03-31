package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.GeoCoordinatesDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.GeoCoordinatesMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.GeoCoordinatesResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface GeoCoordinatesService {
    GeoCoordinatesResponse create(CreateGeoCoordinatesRequest request, Locale locale, String username);
    GeoCoordinatesResponse findById(Long id, Locale locale, String username);
    GeoCoordinatesResponse findAllAsList(Locale locale, String username);
    GeoCoordinatesResponse update(EditGeoCoordinatesRequest request, String username, Locale locale);
    GeoCoordinatesResponse delete(Long id, Locale locale, String username);
    GeoCoordinatesResponse findByMultipleFilters(GeoCoordinatesMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<GeoCoordinatesDto> items);
    byte[] exportToExcel(List<GeoCoordinatesDto> items) throws IOException;
    byte[] exportToPdf(List<GeoCoordinatesDto> items) throws DocumentException;
    ImportSummary importGeoCoordinatesFromCsv(InputStream csvInputStream) throws IOException;
}