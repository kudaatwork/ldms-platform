package projectlx.co.zw.locationsmanagementservice.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AddressDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AddressMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AddressResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface AddressServiceProcessor {
    AddressResponse create(CreateAddressRequest createAddressRequest, Locale locale, String username);
    AddressResponse update(EditAddressRequest editAddressRequest, Locale locale, String username);
    AddressResponse findById(Long id, Locale locale, String username);
    AddressResponse findAllAsAList(Locale locale, String username);
    AddressResponse delete(Long id, Locale locale, String username);
    AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest addressMultipleFiltersRequest, String username, Locale locale);
    byte[] exportToCsv(List<AddressDto> dtoList);
    byte[] exportToExcel(List<AddressDto> dtoList) throws IOException;
    byte[] exportToPdf(List<AddressDto> dtoList) throws DocumentException;
    ImportSummary importFromCsv(InputStream csvInputStream) throws IOException;
}