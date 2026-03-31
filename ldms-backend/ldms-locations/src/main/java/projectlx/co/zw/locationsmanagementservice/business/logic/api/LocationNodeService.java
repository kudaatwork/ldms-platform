package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;

import java.util.Locale;

public interface LocationNodeService {
    LocationNodeResponse create(CreateLocationNodeRequest request, Locale locale, String username);
    LocationNodeResponse update(EditLocationNodeRequest request, Locale locale, String username);
    LocationNodeResponse findById(Long id, Locale locale, String username);
    LocationNodeResponse findByParentId(Long parentId, Locale locale, String username);
    LocationNodeResponse findByMultipleFilters(LocationNodeMultipleFiltersRequest request, Locale locale, String username);
    LocationNodeResponse delete(Long id, Locale locale, String username);
}
