package projectlx.user.management.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectlx.user.management.service.utils.requests.CreateAddressRequest;
import projectlx.user.management.service.utils.requests.EditAddressRequest;
import projectlx.user.management.service.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.AddressResponse;
import projectlx.user.management.service.utils.config.FeignConfig;
import java.util.Locale;

@FeignClient(name = "locations-management-service", url = "${clients.base-url.locationService}", configuration = FeignConfig.class)
public interface LocationsServiceClient {

    @PostMapping("/create")
    AddressResponse create(@RequestBody CreateAddressRequest createAddressRequest,
                          @RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale);

    @PutMapping("/update")
    AddressResponse update(@RequestBody EditAddressRequest editAddressRequest,
                          @RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale);

    @GetMapping("/find-by-id/{id}")
    AddressResponse findById(@PathVariable("id") Long id,
                            @RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale);

    @DeleteMapping("/delete-by-id/{id}")
    AddressResponse delete(@PathVariable("id") Long id,
                          @RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale);

    @GetMapping("/find-by-list")
    AddressResponse findAllAsAList(@RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale);

    @PostMapping("/find-by-multiple-filters")
    AddressResponse findByMultipleFilters(@RequestBody AddressMultipleFiltersRequest addressMultipleFiltersRequest,
                                         @RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale);

    @PostMapping("/export")
    ResponseEntity<byte[]> exportAddresses(@RequestBody AddressMultipleFiltersRequest filters,
                                         @RequestParam("format") String format,
                                         @RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale);

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Object> importAddressesFromCsv(@RequestPart("file") MultipartFile file);
}