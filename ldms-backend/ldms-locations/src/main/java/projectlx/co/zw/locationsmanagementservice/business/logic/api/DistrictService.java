package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.DistrictDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.DistrictMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.DistrictResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface DistrictService {
    DistrictResponse create(CreateDistrictRequest request, Locale locale, String username);
    DistrictResponse findById(Long id, Locale locale, String username);
    DistrictResponse findAllAsList(Locale locale, String username);
    DistrictResponse update(EditDistrictRequest request, String username, Locale locale);
    DistrictResponse delete(Long id, Locale locale, String username);
    DistrictResponse findByMultipleFilters(DistrictMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<DistrictDto> items);
    byte[] exportToExcel(List<DistrictDto> items) throws IOException;
    byte[] exportToPdf(List<DistrictDto> items) throws DocumentException;
    ImportSummary importDistrictFromCsv(InputStream csvInputStream) throws IOException;
}