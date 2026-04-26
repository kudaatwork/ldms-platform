package projectlx.co.zw.locationsmanagementservice.service.rest.frontend;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-locations/v1/frontend/location-node")
@RequiredArgsConstructor
public class LocationNodeFrontendResource {
    private final LocationNodeServiceProcessor locationNodeServiceProcessor;

    @Auditable(action = "CREATE_LOCATION_NODE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "CREATE_LOCATION_NODE.toString())")
    @PostMapping("/create")
    public LocationNodeResponse create(@Valid @RequestBody CreateLocationNodeRequest request,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.create(request, locale, username);
    }

    @Auditable(action = "UPDATE_LOCATION_NODE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "UPDATE_LOCATION_NODE.toString())")
    @PutMapping("/update")
    public LocationNodeResponse update(@Valid @RequestBody EditLocationNodeRequest request,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.update(request, locale, username);
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "VIEW_LOCATION_NODE_BY_ID.toString())")
    @GetMapping("/find-by-id/{id}")
    public LocationNodeResponse findById(@PathVariable("id") Long id,
                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_PARENT")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "VIEW_LOCATION_NODE_BY_PARENT.toString())")
    @GetMapping("/find-by-parent-id/{parentId}")
    public LocationNodeResponse findByParentId(@PathVariable("parentId") Long parentId,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.findByParentId(parentId, locale, username);
    }

    @Auditable(action = "FIND_LOCATION_NODE_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "VIEW_LOCATION_NODE_BY_FILTERS.toString())")
    @PostMapping("/find-by-multiple-filters")
    public LocationNodeResponse findByMultipleFilters(@RequestBody LocationNodeMultipleFiltersRequest request,
                                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.findByMultipleFilters(request, locale, username);
    }

    @Auditable(action = "DELETE_LOCATION_NODE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LocationNodeRoles)." +
            "DELETE_LOCATION_NODE.toString())")
    @DeleteMapping("/delete-by-id/{id}")
    public LocationNodeResponse delete(@PathVariable("id") Long id,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return locationNodeServiceProcessor.delete(id, locale, username);
    }
}
