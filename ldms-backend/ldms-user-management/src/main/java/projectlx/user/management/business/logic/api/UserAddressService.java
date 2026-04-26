package projectlx.user.management.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.utils.dtos.AddressDto;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.utils.responses.AddressResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public interface UserAddressService {
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
