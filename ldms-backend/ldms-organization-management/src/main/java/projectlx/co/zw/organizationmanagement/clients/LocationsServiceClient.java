package projectlx.co.zw.organizationmanagement.clients;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressCreateRequest;
import projectlx.co.zw.organizationmanagement.clients.dto.LocationAddressResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

/**
 * Feign client for ldms-locations system address API (server-to-server address creation).
 */
public interface LocationsServiceClient {

    String ADDRESS_SYSTEM_API_BASE = "/ldms-locations/v1/system/address";

    @PostMapping(ADDRESS_SYSTEM_API_BASE + "/create")
    LocationAddressResponse create(
            @RequestBody LocationAddressCreateRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
