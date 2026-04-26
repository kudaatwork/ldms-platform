package projectlx.user.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import projectlx.user.management.business.logic.api.UserAddressService;
import projectlx.user.management.service.processor.api.UserAddressServiceProcessor;
import projectlx.user.management.utils.dtos.AddressDto;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.utils.responses.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class UserAddressServiceProcessorImpl implements UserAddressServiceProcessor {

    private final UserAddressService userAddressService;
    private static final Logger logger = LoggerFactory.getLogger(UserAddressServiceProcessorImpl.class);

    @Override
    public AddressResponse create(CreateAddressRequest createAddressRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user address : {}", createAddressRequest);

        AddressResponse addressResponse = userAddressService.create(createAddressRequest,
                locale, username);

        logger.info("Outgoing response after creating a user address : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user address by id: {}", id);

        AddressResponse addressResponse = userAddressService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user address by id : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse findAllAsList(String username, Locale locale) {

        logger.info("Incoming request to find all user addresses as a list");

        AddressResponse addressResponse = userAddressService.findAllAsList(username, locale);

        logger.info("Outgoing response after finding all user addresses as a list : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse update(EditAddressRequest editAddressRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user address : {}", editAddressRequest);

        AddressResponse addressResponse = userAddressService.update(editAddressRequest, username, locale);

        logger.info("Outgoing response after updating a user address : {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user address with the id : {}", id);

        AddressResponse addressResponse = userAddressService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user group: {}. Status Code: {}. Message: {}", addressResponse,
                addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest addressMultipleFiltersRequest, String username, Locale locale) {

        logger.info("Incoming request to find a user address using multiple filters : {}", addressMultipleFiltersRequest);

        AddressResponse addressResponse = userAddressService.findByMultipleFilters(addressMultipleFiltersRequest,
                username, locale);

        logger.info("Outgoing response after finding a user address using multiple filters: {}. Status Code: {}. Message: {}",
                addressResponse, addressResponse.getStatusCode(), addressResponse.getMessage());

        return addressResponse;
    }

    @Override
    public byte[] exportToCsv(AddressMultipleFiltersRequest filters, String username, Locale locale) {
        logger.info("Incoming request to export user addresses to CSV using filters: {}", filters);

        AddressResponse addressResponse = userAddressService.findByMultipleFilters(filters, username, locale);

        List<AddressDto> userAddressList = addressResponse.getAddressDtoList() != null
                ? addressResponse.getAddressDtoList()
                : (addressResponse.getAddressDtoPage() != null
                ? addressResponse.getAddressDtoPage().getContent()
                : Collections.emptyList());

        byte[] csvData = userAddressService.exportToCsv(userAddressList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(AddressMultipleFiltersRequest filters, String username, Locale locale) throws IOException {
        logger.info("Incoming request to export user addresses to Excel using filters: {}", filters);

        AddressResponse addressResponse = userAddressService.findByMultipleFilters(filters, username, locale);

        List<AddressDto> userAddressList = addressResponse.getAddressDtoList() != null
                ? addressResponse.getAddressDtoList()
                : (addressResponse.getAddressDtoPage() != null
                ? addressResponse.getAddressDtoPage().getContent()
                : Collections.emptyList());

        byte[] excelData = userAddressService.exportToExcel(userAddressList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(AddressMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException {
        logger.info("Incoming request to export user addresses to PDF using filters: {}", filters);

        AddressResponse addressResponse = userAddressService.findByMultipleFilters(filters, username, locale);

        List<AddressDto> userAddressList = addressResponse.getAddressDtoList() != null
                ? addressResponse.getAddressDtoList()
                : (addressResponse.getAddressDtoPage() != null
                ? addressResponse.getAddressDtoPage().getContent()
                : Collections.emptyList());

        byte[] pdfData = userAddressService.exportToPdf(userAddressList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }
}
