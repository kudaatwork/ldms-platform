package projectlx.co.zw.locationsmanagementservice.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationNodeServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@RestController
@RequestMapping("/ldms-locations/v1/backoffice/location-node")
@RequiredArgsConstructor
public class LocationNodeBackofficeResource {

    private final LocationNodeServiceProcessor locationNodeServiceProcessor;

    @Auditable(action = "CREATE_LOCATION_NODE")
    @PostMapping("/create")
    public LocationNodeResponse create(@Valid @RequestBody CreateLocationNodeRequest request,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return locationNodeServiceProcessor.create(request, locale, "BACKOFFICE");
    }

    @Auditable(action = "UPDATE_LOCATION_NODE")
    @PutMapping("/update")
    public LocationNodeResponse update(@Valid @RequestBody EditLocationNodeRequest request,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return locationNodeServiceProcessor.update(request, locale, "BACKOFFICE");
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_ID")
    @GetMapping("/find-by-id/{id}")
    public LocationNodeResponse findById(@PathVariable("id") Long id,
                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return locationNodeServiceProcessor.findById(id, locale, "BACKOFFICE");
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_PARENT")
    @GetMapping("/find-by-parent-id/{parentId}")
    public LocationNodeResponse findByParentId(@PathVariable("parentId") Long parentId,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return locationNodeServiceProcessor.findByParentId(parentId, locale, "BACKOFFICE");
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_MULTIPLE_FILTERS")
    @PostMapping("/find-by-multiple-filters")
    public LocationNodeResponse findByMultipleFilters(@RequestBody LocationNodeMultipleFiltersRequest request,
                                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return locationNodeServiceProcessor.findByMultipleFilters(request, locale, "BACKOFFICE");
    }

    @Auditable(action = "DELETE_LOCATION_NODE")
    @DeleteMapping("/delete-by-id/{id}")
    public LocationNodeResponse delete(@PathVariable("id") Long id,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return locationNodeServiceProcessor.delete(id, locale, "BACKOFFICE");
    }
}
