package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocationNodeDto;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface LocationNodeService {
    LocationNodeResponse create(CreateLocationNodeRequest request, Locale locale, String username);
    LocationNodeResponse update(EditLocationNodeRequest request, Locale locale, String username);
    LocationNodeResponse findById(Long id, Locale locale, String username);
    LocationNodeResponse findByParentId(Long parentId, Locale locale, String username);
    LocationNodeResponse findByMultipleFilters(LocationNodeMultipleFiltersRequest request, Locale locale, String username);
    /** All non-deleted nodes for list/dropdown endpoints (GET find-by-list). */
    LocationNodeResponse findAllAsList(Locale locale, String username);
    LocationNodeResponse delete(Long id, Locale locale, String username);
    byte[] exportToCsv(List<LocationNodeDto> items);
    byte[] exportToExcel(List<LocationNodeDto> items) throws IOException;
    byte[] exportToPdf(List<LocationNodeDto> items) throws DocumentException;
    ImportSummary importLocationNodeFromCsv(InputStream csvInputStream) throws IOException;
}
