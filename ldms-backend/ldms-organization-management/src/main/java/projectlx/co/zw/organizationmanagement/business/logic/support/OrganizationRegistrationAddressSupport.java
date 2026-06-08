package projectlx.co.zw.organizationmanagement.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.clients.LocationsServiceClient;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressCreateRequest;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressDto;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressResponse;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationRegistrationAddressSupport {

    private final LocationsServiceClient locationsServiceClient;

    /**
     * Resolves {@code locationId} for organisation registration: explicit id, or create address in ldms-locations.
     */
    public Long resolveLocationId(RegisterOrganizationRequest request, Locale locale) {
        if (request.getLocationId() != null && request.getLocationId() > 0) {
            return request.getLocationId();
        }
        String line1 = request.getAddressLine1() != null ? request.getAddressLine1().trim() : "";
        Long suburbId = request.getSuburbId();
        boolean hasAnyAddressInput = StringUtils.hasText(line1)
                || StringUtils.hasText(request.getAddressLine2())
                || StringUtils.hasText(request.getPostalCode())
                || (suburbId != null && suburbId > 0);
        if (!hasAnyAddressInput) {
            return null;
        }
        if (!StringUtils.hasText(line1)
                || !StringUtils.hasText(request.getPostalCode())
                || suburbId == null
                || suburbId < 1) {
            throw new IllegalArgumentException(
                    "Address requires line 1, postal code, and a selected suburb when any address field is provided.");
        }
        LocationAddressCreateRequest create = new LocationAddressCreateRequest();
        create.setLine1(line1);
        if (StringUtils.hasText(request.getAddressLine2())) {
            create.setLine2(request.getAddressLine2().trim());
        }
        if (StringUtils.hasText(request.getPostalCode())) {
            create.setPostalCode(request.getPostalCode().trim());
        }
        create.setSuburbId(suburbId);
        if (request.getCityId() != null && request.getCityId() > 0) {
            create.setCityId(request.getCityId());
        }
        LocationAddressResponse response = locationsServiceClient.create(create, locale);
        if (response == null || !response.isSuccess() || response.getAddressDto() == null
                || response.getAddressDto().getId() == null || response.getAddressDto().getId() < 1) {
            throw new IllegalStateException("Could not create organisation address in the locations service.");
        }
        return response.getAddressDto().getId();
    }

    /**
     * Enriches an {@link OrganizationDto} with full address detail fetched from ldms-locations.
     * Silently returns on any failure so that callers (e.g. {@code getCustomer}) are never broken
     * by a downstream service outage.
     */
    public void enrichOrganizationAddress(OrganizationDto dto, Locale locale) {
        if (dto == null || dto.getLocationId() == null || dto.getLocationId() < 1) {
            return;
        }
        try {
            LocationAddressResponse response = locationsServiceClient.findById(dto.getLocationId(), locale);
            if (response == null || !response.isSuccess() || response.getAddressDto() == null) {
                return;
            }
            LocationAddressDto addr = response.getAddressDto();
            dto.setAddressLine1(addr.getLine1());
            dto.setAddressLine2(addr.getLine2());
            dto.setAddressPostalCode(addr.getPostalCode());
            dto.setAddressSuburbId(addr.getSuburbId());
            dto.setAddressCityId(addr.getCityId());
            dto.setAddressCityName(addr.getCityName());
            dto.setAddressDistrictId(addr.getDistrictId());
            dto.setAddressProvinceId(addr.getProvinceId());
            dto.setAddressCountryId(addr.getCountryId());
        } catch (Exception ex) {
            log.warn("Failed to enrich address for organization locationId={}: {}", dto.getLocationId(), ex.getMessage());
        }
    }
}
