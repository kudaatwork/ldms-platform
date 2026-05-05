package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AddressService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.AddressServiceProcessor;
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

@RequiredArgsConstructor
public class AddressServiceProcessorImpl implements AddressServiceProcessor {

    private final AddressService addressService;
    private final Logger logger = LoggerFactory.getLogger(AddressServiceProcessorImpl.class);

    @Override
    public AddressResponse create(CreateAddressRequest request, Locale locale, String username) {

        logger.info("Incoming request to create an address : {}", request);

        AddressResponse addressResponse = addressService.create(request, locale, username);

        logger.info("Outgoing response after creating an address : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse update(EditAddressRequest request, Locale locale, String username) {
        logger.info("Incoming request to update an address : {}", request);

        AddressResponse addressResponse = addressService.update(request, username, locale);

        logger.info("Outgoing response after updating an address : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find an address by id: {}", id);

        AddressResponse addressResponse = addressService.findById(id, locale, username);

        logger.info("Outgoing response after finding an address by id : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse findAllAsAList(Locale locale, String username) {
        logger.info("Incoming request to find all addresses as a list");

        AddressResponse addressResponse = addressService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all addresses as a list : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete an address with the id : {}", id);

        AddressResponse addressResponse = addressService.delete(id, locale, username);

        logger.info("Outgoing response after deleting an address: {}. Status Code: {}. Message: {}", addressResponse,
                addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find an address using multiple filters : {}", request);

        AddressResponse addressResponse = addressService.findByMultipleFilters(request, username, locale);

        logger.info("Outgoing response after finding an address using multiple filters: {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public byte[] exportToCsv(List<AddressDto> dtoList) {
        logger.info("Incoming request to export addresses to CSV. List size: {}", dtoList.size());

        byte[] csvData = addressService.exportToCsv(dtoList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(List<AddressDto> dtoList) throws IOException {
        logger.info("Incoming request to export addresses to Excel. List size: {}", dtoList.size());

        byte[] excelData = addressService.exportToExcel(dtoList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(List<AddressDto> dtoList) throws DocumentException {
        logger.info("Incoming request to export addresses to PDF. List size: {}", dtoList.size());

        byte[] pdfData = addressService.exportToPdf(dtoList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import addresses from CSV");

        ImportSummary importSummary = addressService.importAddressFromCsv(csvInputStream);

        logger.info("Outgoing response after importing addresses from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.importedCount, importSummary.failed);

        return importSummary;
    }
}
