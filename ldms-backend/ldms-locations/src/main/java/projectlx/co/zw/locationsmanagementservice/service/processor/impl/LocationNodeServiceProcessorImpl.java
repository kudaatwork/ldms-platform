package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationNodeService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationNodeServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class LocationNodeServiceProcessorImpl implements LocationNodeServiceProcessor {
    private final LocationNodeService locationNodeService;

    @Override
    public LocationNodeResponse create(CreateLocationNodeRequest request, Locale locale, String username) {
        log.info("Incoming create location-node request: {}", request);
        return locationNodeService.create(request, locale, username);
    }

    @Override
    public LocationNodeResponse update(EditLocationNodeRequest request, Locale locale, String username) {
        log.info("Incoming update location-node request: {}", request);
        return locationNodeService.update(request, locale, username);
    }

    @Override
    public LocationNodeResponse findById(Long id, Locale locale, String username) {
        return locationNodeService.findById(id, locale, username);
    }

    @Override
    public LocationNodeResponse findByParentId(Long parentId, Locale locale, String username) {
        return locationNodeService.findByParentId(parentId, locale, username);
    }

    @Override
    public LocationNodeResponse findByMultipleFilters(LocationNodeMultipleFiltersRequest request, Locale locale, String username) {
        return locationNodeService.findByMultipleFilters(request, locale, username);
    }

    @Override
    public LocationNodeResponse delete(Long id, Locale locale, String username) {
        return locationNodeService.delete(id, locale, username);
    }
}
