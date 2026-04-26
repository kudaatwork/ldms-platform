package projectlx.user.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.utils.responses.AddressResponse;
import java.io.IOException;
import java.util.Locale;

public interface UserAddressServiceProcessor {
    AddressResponse create(CreateAddressRequest createAddressRequest, Locale locale, String username);
    AddressResponse findById(Long id, Locale locale, String username);
    AddressResponse findAllAsList(String username, Locale locale);
    AddressResponse update(EditAddressRequest editAddressRequest, String username, Locale locale);
    AddressResponse delete(Long id, Locale locale, String username);
    AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest addressMultipleFiltersRequest,
                                          String username, Locale locale);
    byte[] exportToCsv(AddressMultipleFiltersRequest filters, String username, Locale locale);
    byte[] exportToExcel(AddressMultipleFiltersRequest filters, String username, Locale locale) throws IOException;
    byte[] exportToPdf(AddressMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException;
}
