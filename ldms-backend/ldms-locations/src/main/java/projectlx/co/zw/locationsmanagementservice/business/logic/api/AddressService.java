package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AddressDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AddressMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AddressResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface AddressService {
    AddressResponse create(CreateAddressRequest request, Locale locale, String username);
    AddressResponse findById(Long id, Locale locale, String username);
    AddressResponse findAllAsList(Locale locale, String username);
    AddressResponse update(EditAddressRequest request, String username, Locale locale);
    AddressResponse delete(Long id, Locale locale, String username);
    AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest request, String username, Locale locale);
    byte[] exportToCsv(List<AddressDto> items);
    byte[] exportToExcel(List<AddressDto> items) throws IOException;
    byte[] exportToPdf(List<AddressDto> items) throws DocumentException;
    ImportSummary importAddressFromCsv(InputStream csvInputStream) throws IOException;
}