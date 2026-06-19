package projectlx.co.zw.organizationmanagement.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.clients.LocationsServiceClient;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressCreateRequest;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressDto;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressResponse;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.utils.requests.RegisterOrganizationRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateMyOrganizationRequest;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;

import java.util.Locale;
import java.util.Map;

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
     * Resolves {@code locationId} for an organisation self-service profile update.
     * Mirrors {@link #resolveLocationId(RegisterOrganizationRequest, Locale)} but reads from
     * {@link UpdateMyOrganizationRequest}.  Returns {@code null} when no address input is present.
     */
    public Long resolveLocationIdForUpdateMy(UpdateMyOrganizationRequest request, Locale locale) {
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
            dto.setAddressDistrictName(addr.getDistrictName());
            dto.setAddressProvinceId(addr.getProvinceId());
            dto.setAddressProvinceName(addr.getProvinceName());
            dto.setAddressCountryId(addr.getCountryId());
        } catch (Exception ex) {
            log.warn("Failed to enrich address for organization locationId={}: {}", dto.getLocationId(), ex.getMessage());
        }
    }

    /**
     * Head-office branches often have no {@code region} column. After static org-field fallbacks, derive a label
     * from {@code regions_served} or the organisation / branch address in ldms-locations.
     */
    public void enrichHeadOfficeBranchRegion(
            Branch branch,
            BranchDto dto,
            Locale locale,
            Map<Long, String> organizationRegionCache) {
        if (branch == null || dto == null || !branch.isHeadOffice() || StringUtils.hasText(dto.getRegion())) {
            return;
        }
        Organization org = branch.getOrganization();
        if (org != null) {
            String orgRegion = organizationRegionCache.computeIfAbsent(
                    org.getId(), id -> resolveOrganizationRegionLabel(org, locale));
            if (StringUtils.hasText(orgRegion)) {
                dto.setRegion(orgRegion.trim());
                return;
            }
        }
        String branchRegion = resolveAddressRegionLabel(branch.getLocationId(), locale);
        if (StringUtils.hasText(branchRegion)) {
            dto.setRegion(branchRegion.trim());
        }
    }

    public String resolveOrganizationRegionLabel(Organization org, Locale locale) {
        if (org == null) {
            return null;
        }
        String fromRegionsServed = firstRegionFromRegionsServed(org.getRegionsServed());
        if (StringUtils.hasText(fromRegionsServed)) {
            return fromRegionsServed;
        }
        return resolveAddressRegionLabel(org.getLocationId(), locale);
    }

    public String resolveAddressRegionLabel(Long locationId, Locale locale) {
        LocationAddressDto address = fetchAddress(locationId, locale);
        if (address == null) {
            return null;
        }
        return firstNonBlank(address.getProvinceName(), address.getDistrictName(), address.getCityName());
    }

    private LocationAddressDto fetchAddress(Long locationId, Locale locale) {
        if (locationId == null || locationId < 1) {
            return null;
        }
        try {
            LocationAddressResponse response = locationsServiceClient.findById(locationId, locale);
            if (response == null || !response.isSuccess() || response.getAddressDto() == null) {
                return null;
            }
            return response.getAddressDto();
        } catch (Exception ex) {
            log.warn("Failed to resolve address for locationId={}: {}", locationId, ex.getMessage());
            return null;
        }
    }

    private static String firstRegionFromRegionsServed(String regionsServed) {
        if (!StringUtils.hasText(regionsServed)) {
            return null;
        }
        String raw = regionsServed.trim();
        int comma = raw.indexOf(',');
        return comma > 0 ? raw.substring(0, comma).trim() : raw;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
