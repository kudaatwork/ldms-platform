package projectlx.inventory.management.clients;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.inventory.management.utils.requests.CreateAddressRequest;
import projectlx.inventory.management.utils.responses.AddressResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface LocationsServiceClient {

    @PostMapping("/ldms-locations/v1/system/address/create")
    AddressResponse create(@Valid @RequestBody final CreateAddressRequest createAddressRequest,
                           @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                           @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                   defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);
}
