package projectlx.user.management.service.business.logic.impl;

import com.lowagie.text.DocumentException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.auditable.api.UserAddressServiceAuditable;
import projectlx.user.management.service.business.logic.api.UserAddressService;
import projectlx.user.management.service.business.validator.api.UserAddressServiceValidator;
import projectlx.user.management.service.clients.LocationsServiceClient;
import projectlx.user.management.service.model.Address;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.repository.UserAddressRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.utils.dtos.AddressDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateAddressRequest;
import projectlx.user.management.service.utils.requests.EditAddressRequest;
import projectlx.user.management.service.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressServiceValidator userAddressServiceValidator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final UserAddressServiceAuditable userAddressServiceAuditable;
    private final LocationsServiceClient locationsServiceClient;

    @Override
    @Transactional
    @CircuitBreaker(name = "location-service", fallbackMethod = "fallbackCreateAddress")
    public AddressResponse create(CreateAddressRequest createAddressRequest, Locale locale, String username) {

        String message;

        try {

            ValidatorDto validatorDto = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

            if (validatorDto == null || !validatorDto.getSuccess()) {

                message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ADDRESS_INVALID_REQUEST.getCode(),
                        new String[]{}, locale);

                return buildAddressResponseWithErrors(400, false, message, null,
                        null, validatorDto != null ? validatorDto.getErrorMessages() : null);
            }

            // Step 1: Create the address in the external Location Service
            log.info("Creating address in Location Service: {}", createAddressRequest);
            AddressResponse locationServiceResponse = locationsServiceClient.create(createAddressRequest, locale);

            // Step 2: Check if the external call was successful
            if (!locationServiceResponse.isSuccess() || locationServiceResponse.getAddressDto() == null) {

                message = messageService.getMessage(I18Code.MESSAGE_LOCATION_SERVICE_ERROR.getCode(), new String[]{}, locale);

                return buildAddressResponse(500, false, message, null, null);
            }

            // Step 3: Create the local user_address reference entity
            Long newLocationId = locationServiceResponse.getAddressDto().getId();

            Optional<Address> existingAddress = userAddressRepository.findByLocationAddressIdAndEntityStatusNot(newLocationId,
                    EntityStatus.DELETED);

            if (existingAddress.isEmpty()) {

                Address userAddress = new Address();
                userAddress.setLocationAddressId(newLocationId);
                userAddressServiceAuditable.create(userAddress, locale, username);
                log.info("Created local user_address reference for location_address_id: {}", newLocationId);
            } else {
                log.info("Local user_address reference for location_address_id: {} already exists.", newLocationId);
            }

            // Step 4: Return the ORIGINAL response from the locationsServiceClient. This is the key fix.
            return locationServiceResponse;

        } catch (Exception e) {
            log.error("Error creating address: ", e);
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_CREATION_FAILED.getCode(), new String[]{}, locale);
            return buildAddressResponse(500, false, message, null, null);
        }
    }

    @Override
    @Cacheable(value = "addressDetails", key = "#id")
    @CircuitBreaker(name = "location-service", fallbackMethod = "fallbackFindById")
    public AddressResponse findById(Long id, Locale locale, String username) {

        String message = "";

        try {
            ValidatorDto validatorDto = userAddressServiceValidator.isIdValid(id, locale);

            if (validatorDto == null || !validatorDto.getSuccess()) {
                message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_ID_INVALID.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(400, false, message, null, null);
            }

            Optional<Address> userAddressOptional = userAddressRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

            if (userAddressOptional.isEmpty()) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(404, false, message, null, null);
            }

            Address userAddress = userAddressOptional.get();
            userAddress = populateAddressDetails(userAddress);

            AddressDto addressDto = convertToAddressDto(userAddress);

            message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            return buildAddressResponse(200, true, message, addressDto, null);

        } catch (Exception e) {
            log.error("Error finding address by ID {}: ", id, e);
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_RETRIEVAL_FAILED.getCode(),
                    new String[]{}, locale);
            return buildAddressResponse(500, false, message, null, null);
        }
    }

    @Override
    public AddressResponse findAllAsList(String username, Locale locale) {
        String message = "";

        try {
            List<Address> userAddresses = userAddressRepository.findByEntityStatusNot(EntityStatus.DELETED);

            if (userAddresses.isEmpty()) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(404, false, message, null, null);
            }

            // Populate address details for each address
            List<AddressDto> addressDtoList = userAddresses.stream()
                    .map(this::populateAddressDetails)
                    .map(this::convertToAddressDto)
                    .toList();

            message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            return buildAddressResponse(200, true, message, null, addressDtoList);

        } catch (Exception e) {
            log.error("Error finding all addresses: ", e);
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_RETRIEVAL_FAILED.getCode(),
                    new String[]{}, locale);
            return buildAddressResponse(500, false, message, null, null);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "addressDetails", key = "#editAddressRequest.id")
    @CircuitBreaker(name = "location-service", fallbackMethod = "fallbackUpdateAddress")
    public AddressResponse update(EditAddressRequest editAddressRequest, String username, Locale locale) {
        String message = "";

        try {
            ValidatorDto validatorDto = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

            if (validatorDto == null || !validatorDto.getSuccess()) {
                message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ADDRESS_INVALID_REQUEST.getCode(),
                        new String[]{}, locale);
                return buildAddressResponseWithErrors(400, false, message, null, null,
                        validatorDto != null ? validatorDto.getErrorMessages() : null);
            }

            Optional<Address> userAddressOptional = userAddressRepository.findByIdAndEntityStatusNot(
                    editAddressRequest.getId(), EntityStatus.DELETED);

            if (userAddressOptional.isEmpty()) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(404, false, message, null, null);
            }

            Address userAddress = userAddressOptional.get();

            // Update address in Location Service
            log.info("Updating address in Location Service: {}", editAddressRequest);
            AddressResponse locationServiceResponse = locationsServiceClient.update(editAddressRequest, locale);

            if (!locationServiceResponse.isSuccess()) {
                message = messageService.getMessage(I18Code.MESSAGE_LOCATION_SERVICE_ERROR.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(500, false, message, null, null);
            }

            // Update user address if location address ID changed
            if (editAddressRequest.getLocationAddressId() != null) {
                userAddress.setLocationAddressId(editAddressRequest.getLocationAddressId());
            }

            Address updatedUserAddress = userAddressServiceAuditable.update(userAddress, locale, username);
            updatedUserAddress = populateAddressDetails(updatedUserAddress);

            AddressDto addressDto = convertToAddressDto(updatedUserAddress);

            message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_UPDATED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            return buildAddressResponse(201, true, message, addressDto, null);

        } catch (Exception e) {
            log.error("Error updating address: ", e);
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_UPDATE_FAILED.getCode(),
                    new String[]{}, locale);
            return buildAddressResponse(500, false, message, null, null);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "addressDetails", key = "#id")
    @CircuitBreaker(name = "location-service", fallbackMethod = "fallbackDeleteAddress")
    public AddressResponse delete(Long id, Locale locale, String username) {
        String message = "";

        try {
            ValidatorDto validatorDto = userAddressServiceValidator.isIdValid(id, locale);

            if (validatorDto == null || !validatorDto.getSuccess()) {
                message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_ID_INVALID.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(400, false, message, null, null);
            }

            Optional<Address> userAddressOptional = userAddressRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

            if (userAddressOptional.isEmpty()) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(404, false, message, null, null);
            }

            Address userAddress = userAddressOptional.get();

            // Delete address in Location Service
            log.info("Deleting address in Location Service with ID: {}", userAddress.getLocationAddressId());
            AddressResponse locationServiceResponse = locationsServiceClient.delete(userAddress.getLocationAddressId(), locale);

            if (!locationServiceResponse.isSuccess()) {
                log.warn("Failed to delete address in Location Service, proceeding with user address deletion");
            }

            // Soft delete the user address
            userAddress.setEntityStatus(EntityStatus.DELETED);
            Address deletedUserAddress = userAddressServiceAuditable.delete(userAddress, locale);

            AddressDto addressDto = convertToAddressDto(deletedUserAddress);

            message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_DELETED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            return buildAddressResponse(200, true, message, addressDto, null);

        } catch (Exception e) {
            log.error("Error deleting address: ", e);
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_DELETION_FAILED.getCode(),
                    new String[]{}, locale);
            return buildAddressResponse(500, false, message, null, null);
        }
    }

    @Override
    @CircuitBreaker(name = "location-service", fallbackMethod = "fallbackFindByMultipleFilters")
    public AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest addressMultipleFiltersRequest,
                                                 String username, Locale locale) {
        String message = "";

        try {
            ValidatorDto validatorDto = userAddressServiceValidator.isRequestValidToRetrieveAddressesByMultipleFilters(
                    addressMultipleFiltersRequest, locale);

            if (validatorDto == null || !validatorDto.getSuccess()) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(400, false, message, null, null);
            }

            // Search in Location Service
            log.info("Searching addresses in Location Service with filters: {}", addressMultipleFiltersRequest);
            AddressResponse locationServiceResponse = locationsServiceClient.findByMultipleFilters(
                    addressMultipleFiltersRequest, locale);

            if (!locationServiceResponse.isSuccess()) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(404, false, message, null, null);
            }

            // Check if we have data in either addressDtoList or addressDtoPage
            List<AddressDto> addressList = null;

            if (locationServiceResponse.getAddressDtoList() != null && !locationServiceResponse.getAddressDtoList().isEmpty()) {
                // Data returned as a list
                addressList = locationServiceResponse.getAddressDtoList();
            } else if (locationServiceResponse.getAddressDtoPage() != null &&
                    locationServiceResponse.getAddressDtoPage().getContent() != null &&
                    !locationServiceResponse.getAddressDtoPage().getContent().isEmpty()) {
                // Data returned as a page - extract the content
                addressList = locationServiceResponse.getAddressDtoPage().getContent();
            }

            if (addressList == null || addressList.isEmpty()) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode(),
                        new String[]{}, locale);
                return buildAddressResponse(404, false, message, null, null);
            }

            message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            return buildAddressResponse(200, true, message, null, addressList);

        } catch (Exception e) {
            log.error("Error finding addresses by filters: ", e);
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_RETRIEVAL_FAILED.getCode(),
                    new String[]{}, locale);
            return buildAddressResponse(500, false, message, null, null);
        }
    }

    @Override
    public byte[] exportToCsv(List<AddressDto> userAddresses) {
        try {
            if (userAddresses == null || userAddresses.isEmpty()) {
                return "No addresses available for export".getBytes(StandardCharsets.UTF_8);
            }

            log.info("Exporting {} user addresses to CSV", userAddresses.size());
            return generateCsvExport(userAddresses);

        } catch (Exception e) {
            log.error("Error exporting addresses to CSV: ", e);
            return "Error occurred during CSV export".getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public byte[] exportToExcel(List<AddressDto> userAddresses) throws IOException {
        try {
            if (userAddresses == null || userAddresses.isEmpty()) {
                throw new IOException("No addresses available for export");
            }

            log.info("Exporting {} user addresses to Excel", userAddresses.size());
            return generateExcelExport(userAddresses);

        } catch (Exception e) {
            log.error("Error exporting addresses to Excel: ", e);
            throw new IOException("Failed to export addresses to Excel", e);
        }
    }

    @Override
    public byte[] exportToPdf(List<AddressDto> userAddresses) throws DocumentException {
        try {
            if (userAddresses == null || userAddresses.isEmpty()) {
                throw new DocumentException("No addresses available for export");
            }

            log.info("Exporting {} user addresses to PDF", userAddresses.size());
            return generatePdfExport(userAddresses);

        } catch (Exception e) {
            log.error("Error exporting addresses to PDF: ", e);
            throw new DocumentException("Failed to export addresses to PDF: " + e.getMessage());
        }
    }

    /**
     * Generate CSV export directly from user addresses
     */
    private byte[] generateCsvExport(List<AddressDto> addresses) {
        String[] headers = {"ID", "LINE 1", "LINE 2", "POSTAL CODE", "SUBURB ID", "GEO COORDINATES ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"};

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", headers)).append("\n");

        for (AddressDto address : addresses) {
            sb.append(address.getId() != null ? address.getId() : "").append(",")
                    .append(safe(address.getLine1())).append(",")
                    .append(safe(address.getLine2())).append(",")
                    .append(safe(address.getPostalCode())).append(",")
                    .append(address.getSuburbId() != null ? address.getSuburbId() : "").append(",")
                    .append(address.getGeoCoordinatesId() != null ? address.getGeoCoordinatesId() : "").append(",")
                    .append(address.getCreatedAt() != null ? address.getCreatedAt().toString() : "").append(",")
                    .append(address.getUpdatedAt() != null ? address.getUpdatedAt().toString() : "").append(",")
                    .append(address.getEntityStatus() != null ? address.getEntityStatus().toString() : "").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate Excel export directly from user addresses
     */
    private byte[] generateExcelExport(List<AddressDto> addresses) throws IOException {
        String[] headers = {"ID", "LINE 1", "LINE 2", "POSTAL CODE", "SUBURB ID", "GEO COORDINATES ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"};

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Addresses");

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowIdx = 1;
        for (AddressDto address : addresses) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(address.getId() != null ? address.getId().toString() : "");
            row.createCell(1).setCellValue(safe(address.getLine1()));
            row.createCell(2).setCellValue(safe(address.getLine2()));
            row.createCell(3).setCellValue(safe(address.getPostalCode()));
            row.createCell(4).setCellValue(address.getSuburbId() != null ? address.getSuburbId().toString() : "");
            row.createCell(5).setCellValue(address.getGeoCoordinatesId() != null ? address.getGeoCoordinatesId().toString() : "");
            row.createCell(6).setCellValue(address.getCreatedAt() != null ? address.getCreatedAt().toString() : "");
            row.createCell(7).setCellValue(address.getUpdatedAt() != null ? address.getUpdatedAt().toString() : "");
            row.createCell(8).setCellValue(address.getEntityStatus() != null ? address.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    /**
     * Generate PDF export directly from user addresses
     */
    private byte[] generatePdfExport(List<AddressDto> addresses) throws DocumentException {
        String[] headers = {"ID", "LINE 1", "LINE 2", "POSTAL CODE", "SUBURB ID", "GEO COORDINATES ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"};

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("USER ADDRESS EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(headers.length);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (AddressDto address : addresses) {
            table.addCell(address.getId() != null ? address.getId().toString() : "");
            table.addCell(safe(address.getLine1()));
            table.addCell(safe(address.getLine2()));
            table.addCell(safe(address.getPostalCode()));
            table.addCell(address.getSuburbId() != null ? address.getSuburbId().toString() : "");
            table.addCell(address.getGeoCoordinatesId() != null ? address.getGeoCoordinatesId().toString() : "");
            table.addCell(address.getCreatedAt() != null ? address.getCreatedAt().toString() : "");
            table.addCell(address.getUpdatedAt() != null ? address.getUpdatedAt().toString() : "");
            table.addCell(address.getEntityStatus() != null ? address.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    /**
     * Utility method to safely handle null strings
     */
    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    // Helper methods

    /**
     * Populate address details from Location Service
     */
    @Cacheable(value = "locationAddressDetails", key = "#address.locationAddressId")
    public Address populateAddressDetails(Address address) {

        if (address.getLocationAddressId() != null) {
            try {
                log.debug("Fetching address details for location address ID: {}", address.getLocationAddressId());

                AddressResponse locationResponse = locationsServiceClient.findById(address.getLocationAddressId(), Locale.ENGLISH);

                if (locationResponse.isSuccess() && locationResponse.getAddressDto() != null) {
                    address.setAddressDetails(locationResponse.getAddressDto());
                    log.debug("Successfully fetched address details for location address ID: {}", address.getLocationAddressId());
                } else {
                    log.warn("Failed to fetch address details for location address ID: {}. Response: statusCode={}, success={}, message='{}'",
                            address.getLocationAddressId(),
                            locationResponse.getStatusCode(),
                            locationResponse.isSuccess(),
                            locationResponse.getMessage());

                    // Create fallback address details with clear indication of the problem
                    AddressDto fallbackAddress = new AddressDto();
                    fallbackAddress.setId(address.getLocationAddressId());
                    fallbackAddress.setLine1("[MISSING] Location address ID " + address.getLocationAddressId() + " not found");
                    fallbackAddress.setLine2("Please check data integrity");
                    fallbackAddress.setPostalCode("");
                    address.setAddressDetails(fallbackAddress);
                }
            } catch (Exception e) {
                log.error("Error fetching address details for location address ID: {}. Error: {}",
                        address.getLocationAddressId(), e.getMessage(), e);

                // Create fallback address details
                AddressDto fallbackAddress = new AddressDto();
                fallbackAddress.setId(address.getLocationAddressId());
                fallbackAddress.setLine1("[ERROR] Failed to fetch address details");
                fallbackAddress.setLine2("Error: " + e.getMessage());
                fallbackAddress.setPostalCode("");
                address.setAddressDetails(fallbackAddress);
            }
        } else {
            log.warn("User address ID {} has null locationAddressId", address.getId());

            // Create fallback for null locationAddressId
            AddressDto fallbackAddress = new AddressDto();
            fallbackAddress.setLine1("[NULL] No location address ID set");
            fallbackAddress.setLine2("User address not properly linked");
            fallbackAddress.setPostalCode("");
            address.setAddressDetails(fallbackAddress);
        }
        return address;
    }

    /**
     * Convert Address entity to AddressDto
     */
    private AddressDto convertToAddressDto(Address address) {

        AddressDto addressDto = new AddressDto();
        addressDto.setId(address.getId());
        addressDto.setLocationAddressId(address.getLocationAddressId());

        if (address.getAddressDetails() != null) {
            // Copy details from the fetched address
            AddressDto details = address.getAddressDetails();
            addressDto.setLine1(details.getLine1());
            addressDto.setLine2(details.getLine2());
            addressDto.setPostalCode(details.getPostalCode());
            addressDto.setSuburbId(details.getSuburbId());
            addressDto.setGeoCoordinatesId(details.getGeoCoordinatesId());
        }

        return addressDto;
    }

    // Fallback methods for circuit breaker

    public AddressResponse fallbackCreateAddress(CreateAddressRequest request, Locale locale, String username, Exception ex) {
        log.warn("Location service unavailable for create address operation: {}", ex.getMessage());
        String message = messageService.getMessage(I18Code.MESSAGE_LOCATION_SERVICE_UNAVAILABLE.getCode(),
                new String[]{}, locale);
        return buildAddressResponse(503, false, message, null, null);
    }

    public AddressResponse fallbackFindById(Long id, Locale locale, String username, Exception ex) {
        log.warn("Location service unavailable for find address by ID operation: {}", ex.getMessage());
        String message = messageService.getMessage(I18Code.MESSAGE_LOCATION_SERVICE_UNAVAILABLE.getCode(),
                new String[]{}, locale);
        return buildAddressResponse(503, false, message, null, null);
    }

    public AddressResponse fallbackUpdateAddress(EditAddressRequest request, String username, Locale locale, Exception ex) {
        log.warn("Location service unavailable for update address operation: {}", ex.getMessage());
        String message = messageService.getMessage(I18Code.MESSAGE_LOCATION_SERVICE_UNAVAILABLE.getCode(),
                new String[]{}, locale);
        return buildAddressResponse(503, false, message, null, null);
    }

    public AddressResponse fallbackDeleteAddress(Long id, Locale locale, String username, Exception ex) {
        log.warn("Location service unavailable for delete address operation: {}", ex.getMessage());
        String message = messageService.getMessage(I18Code.MESSAGE_LOCATION_SERVICE_UNAVAILABLE.getCode(),
                new String[]{}, locale);
        return buildAddressResponse(503, false, message, null, null);
    }

    public AddressResponse fallbackFindByMultipleFilters(AddressMultipleFiltersRequest request, String username,
                                                         Locale locale, Exception ex) {
        log.warn("Location service unavailable for find by filters operation: {}", ex.getMessage());
        String message = messageService.getMessage(I18Code.MESSAGE_LOCATION_SERVICE_UNAVAILABLE.getCode(),
                new String[]{}, locale);
        return buildAddressResponse(503, false, message, null, null);
    }

    // Response builders

    private AddressResponse buildAddressResponse(int statusCode, boolean isSuccess, String message,
                                                 AddressDto addressDto, List<AddressDto> addressDtoList) {
        AddressResponse response = new AddressResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setAddressDto(addressDto);
        response.setAddressDtoList(addressDtoList);
        return response;
    }

    private AddressResponse buildAddressResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                           AddressDto addressDto, List<AddressDto> addressDtoList,
                                                           List<String> errorMessages) {
        AddressResponse response = buildAddressResponse(statusCode, isSuccess, message, addressDto, addressDtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }
}
