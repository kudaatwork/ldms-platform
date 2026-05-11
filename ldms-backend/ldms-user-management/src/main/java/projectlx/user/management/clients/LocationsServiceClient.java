package projectlx.user.management.clients;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.AddressMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.utils.responses.AddressResponse;
import java.util.Locale;

/**
 * Feign client for Location Service system address API.
 * Base URL (host:port) is supplied by {@link projectlx.user.management.config.LocationsServiceFeignConfiguration}.
 * <p>Class-level {@code @RequestMapping} is not supported on Feign interfaces; use full paths per method.</p>
 */
public interface LocationsServiceClient {

    String ADDRESS_SYSTEM_API_BASE = "/ldms-locations/v1/system/address";

    @PostMapping(ADDRESS_SYSTEM_API_BASE + "/create")
    AddressResponse create(@RequestBody CreateAddressRequest createAddressRequest,
                          @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PutMapping(ADDRESS_SYSTEM_API_BASE + "/update")
    AddressResponse update(@RequestBody EditAddressRequest editAddressRequest,
                          @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping(ADDRESS_SYSTEM_API_BASE + "/find-by-id/{id}")
    AddressResponse findById(@PathVariable("id") Long id,
                            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @DeleteMapping(ADDRESS_SYSTEM_API_BASE + "/delete-by-id/{id}")
    AddressResponse delete(@PathVariable("id") Long id,
                          @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping(ADDRESS_SYSTEM_API_BASE + "/find-by-list")
    AddressResponse findAllAsAList(@RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping(ADDRESS_SYSTEM_API_BASE + "/find-by-multiple-filters")
    AddressResponse findByMultipleFilters(@RequestBody AddressMultipleFiltersRequest addressMultipleFiltersRequest,
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping(ADDRESS_SYSTEM_API_BASE + "/export")
    ResponseEntity<byte[]> exportAddresses(@RequestBody AddressMultipleFiltersRequest filters,
                                         @RequestParam("format") String format,
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping(value = ADDRESS_SYSTEM_API_BASE + "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Object> importAddressesFromCsv(@RequestPart("file") MultipartFile file);
}