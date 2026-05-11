package projectlx.user.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.model.Address;
import projectlx.user.management.utils.dtos.AddressDto;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.utils.responses.AddressResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public interface UserAddressService {
    /**
     * Builds an {@link AddressDto} from a persisted {@link Address} entity, merging
     * {@link Address#getAddressDetails()} when present (e.g. after a location-service fetch).
     * Prefer this over {@code ModelMapper.map(address, AddressDto.class)} to avoid null-source
     * errors from the transient {@code addressDetails} field.
     */
    AddressDto toAddressDto(Address address);

    AddressResponse create(CreateAddressRequest createAddressRequest, Locale locale, String username);
    AddressResponse findById(Long id, Locale locale, String username);
    AddressResponse findAllAsList(String username, Locale locale);
    AddressResponse update(EditAddressRequest editAddressRequest, String username, Locale locale);
    AddressResponse delete(Long id, Locale locale, String username);
    AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest addressMultipleFiltersRequest,
                                          String username, Locale locale);
    byte[] exportToCsv(List<AddressDto> userAddresses);
    byte[] exportToExcel(List<AddressDto> userAddresses) throws IOException;
    byte[] exportToPdf(List<AddressDto> userAddresses) throws DocumentException;
}
