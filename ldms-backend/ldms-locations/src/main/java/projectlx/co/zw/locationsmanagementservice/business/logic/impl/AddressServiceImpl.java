package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

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
import lombok.RequiredArgsConstructor;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AddressServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AddressService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.AddressServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.AddressRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.AddressSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AddressCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AddressDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AddressMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AddressResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressServiceValidator addressServiceValidator;
    private final AddressRepository addressRepository;
    private final SuburbRepository suburbRepository;
    private final GeoCoordinatesRepository geoCoordinatesRepository;
    private final AddressServiceAuditable addressServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "LINE 1", "LINE 2", "POSTAL CODE", "SUBURB", "DISTRICT", "PROVINCE", "COUNTRY", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    private static final String[] CSV_HEADERS = {
            "LINE 1", "LINE 2", "POSTAL CODE", "SUBURB ID"
    };

    @Override
    public AddressResponse create(CreateAddressRequest createAddressRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = addressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_ADDRESS_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildAddressResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        // Ensure suburbId is provided
        if (createAddressRequest.getSuburbId() == null) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_ADDRESS_SUBURB_ID_MISSING.getCode(), new String[]{},
                    locale);

            return buildAddressResponse(400, false, message, null, null,
                    null);
        }

        // Check if the suburb exists
        Optional<Suburb> suburbOptional = suburbRepository.findByIdAndEntityStatusNot(createAddressRequest.getSuburbId(),
                EntityStatus.DELETED);
        
        if (suburbOptional.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SUBURB_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAddressResponse(404, false, message, null, null,
                    null);
        }

        // Check if a similar address already exists with improved check
        List<Address> existingAddresses = addressRepository.findAll().stream()
                .filter(address -> address.getEntityStatus() != EntityStatus.DELETED &&
                        address.getLine1().equalsIgnoreCase(createAddressRequest.getLine1()) &&
                        address.getSuburb() != null &&
                        address.getSuburb().getId().equals(createAddressRequest.getSuburbId()))
                .collect(Collectors.toList());

        if (!existingAddresses.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildAddressResponse(400, false, message, null, null,
                    null);
        }

        // Create new address
        Address addressToBeSaved = new Address();
        addressToBeSaved.setLine1(createAddressRequest.getLine1());
        addressToBeSaved.setLine2(createAddressRequest.getLine2());
        addressToBeSaved.setPostalCode(createAddressRequest.getPostalCode());
        
        // Set suburb (we already verified it exists)
        addressToBeSaved.setSuburb(suburbOptional.get());
        if ((createAddressRequest.getLatitude() == null) != (createAddressRequest.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildAddressResponse(400, false, message, null, null, null);
        }

        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                createAddressRequest.getGeoCoordinatesId(),
                createAddressRequest.getLatitude(),
                createAddressRequest.getLongitude());
        if (createAddressRequest.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildAddressResponse(400, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            addressToBeSaved.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        Address addressSaved = addressServiceAuditable.create(addressToBeSaved, locale, username);

        AddressDto addressDtoReturned = convertAddressToDto(addressSaved);

        message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAddressResponse(201, true, message, addressDtoReturned, null,
                null);
    }

    @Override
    public AddressResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = addressServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildAddressResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Address> addressRetrieved = addressRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (addressRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAddressResponse(404, false, message, null, null,
                    null);
        }

        Address addressReturned = addressRetrieved.get();
        AddressDto addressDto = convertAddressToDto(addressReturned);

        message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAddressResponse(200, true, message, addressDto, null,
                null);
    }

    @Override
    public AddressResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<Address> addressList = addressRepository.findAll().stream()
                .filter(address -> address.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if(addressList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildAddressResponse(404, false, message, null,
                    null, null);
        }

        List<AddressDto> addressDtoList = addressList.stream()
                .map(this::convertAddressToDto)
                .collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildAddressResponse(200, true, message, null, addressDtoList,
                null);
    }

    @Override
    public AddressResponse update(EditAddressRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = addressServiceValidator.isRequestValidForEditing(request, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_ADDRESS_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildAddressResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Address> addressRetrieved = addressRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);

        if (addressRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAddressResponse(404, false, message, null, null,
                    null);
        }

        Address addressToBeEdited = addressRetrieved.get();

        // Update address properties from request
        addressToBeEdited.setLine1(request.getLine1());
        addressToBeEdited.setLine2(request.getLine2());
        addressToBeEdited.setPostalCode(request.getPostalCode());

        // Handle suburb if provided
        if (request.getSuburbId() != null) {
            Optional<Suburb> suburbOptional = suburbRepository.findByIdAndEntityStatusNot(request.getSuburbId(),
                    EntityStatus.DELETED);
            if (suburbOptional.isPresent()) {
                addressToBeEdited.setSuburb(suburbOptional.get());
            }
        }

        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildAddressResponse(400, false, message, null, null, null);
        }
        Optional<GeoCoordinates> geoCoordinatesOptional = resolveGeoCoordinates(
                request.getGeoCoordinatesId(), request.getLatitude(), request.getLongitude());
        if (request.getGeoCoordinatesId() != null && geoCoordinatesOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_GEO_COORDINATES_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildAddressResponse(400, false, message, null, null, null);
        }
        if (geoCoordinatesOptional.isPresent()) {
            addressToBeEdited.setGeoCoordinates(geoCoordinatesOptional.get());
        }

        Address addressEdited = addressServiceAuditable.update(addressToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        AddressDto addressDtoReturned = convertAddressToDto(addressEdited);

        message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAddressResponse(200, true, message, addressDtoReturned, null,
                null);
    }

    @Override
    public AddressResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = addressServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildAddressResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Address> addressRetrieved = addressRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (addressRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildAddressResponse(404, false, message, null, null,
                    null);
        }

        Address addressToBeDeleted = addressRetrieved.get();
        addressToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        Address addressDeleted = addressServiceAuditable.delete(addressToBeDeleted, locale);

        AddressDto addressDtoReturned = convertAddressToDto(addressDeleted);

        message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildAddressResponse(200, true, message, addressDtoReturned, null,
                null);
    }

    @Override
    public AddressResponse findByMultipleFilters(AddressMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<Address> spec = null;
        spec = addToSpec(spec, AddressSpecification::deleted);

        if (request == null) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_ADDRESS_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            List<String> errorMessages = new ArrayList<>();
            errorMessages.add("Request cannot be null");

            return buildAddressResponseWithErrors(400, false, message, null, null,
                    null, errorMessages);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (isNotEmpty(request.getLine1())) {
            spec = addToSpec(request.getLine1(), spec, AddressSpecification::line1Like);
        }

        if (isNotEmpty(request.getLine2())) {
            spec = addToSpec(request.getLine2(), spec, AddressSpecification::line2Like);
        }

        if (isNotEmpty(request.getPostalCode())) {
            spec = addToSpec(request.getPostalCode(), spec, AddressSpecification::postalCodeLike);
        }

        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, AddressSpecification::any);
        }

        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, AddressSpecification::hasEntityStatus);
        }

        Page<Address> result = addressRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildAddressResponse(404, false, message, null, null, null);
        }

        Page<AddressDto> addressDtoPage = convertAddressEntityToAddressDto(result);

        // Also provide the list for export functionality
        List<AddressDto> addressDtoList = addressDtoPage.getContent();

        message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildAddressResponse(200, true, message, null, addressDtoList, addressDtoPage);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Optional<GeoCoordinates> resolveGeoCoordinates(Long geoCoordinatesId, BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                return Optional.empty();
            }
            GeoCoordinates geoCoordinates = new GeoCoordinates();
            geoCoordinates.setLatitude(latitude);
            geoCoordinates.setLongitude(longitude);
            GeoCoordinates saved = geoCoordinatesRepository.save(geoCoordinates);
            return Optional.of(saved);
        }
        if (geoCoordinatesId == null) {
            return Optional.empty();
        }
        return geoCoordinatesRepository.findByIdAndEntityStatusNot(geoCoordinatesId, EntityStatus.DELETED);
    }

    private Specification<Address> addToSpec(Specification<Address> spec,
                                           Function<EntityStatus, Specification<Address>> predicateMethod) {
        Specification<Address> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<Address> addToSpec(final String aString, Specification<Address> spec, Function<String,
            Specification<Address>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<Address> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<Address> addToSpec(final EntityStatus entityStatus, Specification<Address> spec, Function<EntityStatus,
            Specification<Address>> predicateMethod) {
        if (entityStatus != null) {
            Specification<Address> localSpec = Specification.where(predicateMethod.apply(entityStatus));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<AddressDto> convertAddressEntityToAddressDto(Page<Address> addressPage) {

        List<Address> addressList = addressPage.getContent(); // Get the content first
        List<AddressDto> addressDtoList = new ArrayList<>();

        // Fix: Iterate over the content list, not the page object
        for (Address address : addressList) {
            AddressDto addressDto = convertAddressToDto(address);
            addressDtoList.add(addressDto);
        }

        int page = addressPage.getNumber();
        int size = addressPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableAddresses = PageRequest.of(page, size);

        return new PageImpl<>(addressDtoList, pageableAddresses, addressPage.getTotalElements());
    }

    private AddressDto convertAddressToDto(Address address) {
        AddressDto addressDto = modelMapper.map(address, AddressDto.class);

        if (address.getSuburb() != null) {
            addressDto.setSuburbId(address.getSuburb().getId());
            addressDto.setSuburbName(address.getSuburb().getName());

            if (address.getSuburb().getDistrict() != null) {
                addressDto.setDistrictId(address.getSuburb().getDistrict().getId());
                addressDto.setDistrictName(address.getSuburb().getDistrict().getName());

                if (address.getSuburb().getDistrict().getProvince() != null) {
                    addressDto.setProvinceId(address.getSuburb().getDistrict().getProvince().getId());
                    addressDto.setProvinceName(address.getSuburb().getDistrict().getProvince().getName());

                    if (address.getSuburb().getDistrict().getProvince().getCountry() != null) {
                        addressDto.setCountryId(address.getSuburb().getDistrict().getProvince().getCountry().getId());
                        addressDto.setCountryName(address.getSuburb().getDistrict().getProvince().getCountry().getName());
                    }
                }
            }
        }

        return addressDto;
    }

    @Override
    public byte[] exportToCsv(List<AddressDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (AddressDto address : items) {
            sb.append(address.getId()).append(",")
                    .append(safe(address.getLine1())).append(",")
                    .append(safe(address.getLine2())).append(",")
                    .append(safe(address.getPostalCode())).append(",")
                    .append(safe(address.getSuburbName())).append(",")
                    .append(safe(address.getDistrictName())).append(",")
                    .append(safe(address.getProvinceName())).append(",")
                    .append(safe(address.getCountryName())).append(",")
                    .append(address.getCreatedAt()).append(",")
                    .append(address.getUpdatedAt()).append(",")
                    .append(address.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<AddressDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Addresses");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (AddressDto address : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(address.getId());
            row.createCell(1).setCellValue(safe(address.getLine1()));
            row.createCell(2).setCellValue(safe(address.getLine2()));
            row.createCell(3).setCellValue(safe(address.getPostalCode()));
            row.createCell(4).setCellValue(safe(address.getSuburbName()));
            row.createCell(5).setCellValue(safe(address.getDistrictName()));
            row.createCell(6).setCellValue(safe(address.getProvinceName()));
            row.createCell(7).setCellValue(safe(address.getCountryName()));
            row.createCell(8).setCellValue(address.getCreatedAt() != null ? address.getCreatedAt().toString() : "");
            row.createCell(9).setCellValue(address.getUpdatedAt() != null ? address.getUpdatedAt().toString() : "");
            row.createCell(10).setCellValue(address.getEntityStatus() != null ? address.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<AddressDto> items) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("ADDRESS EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);

        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (AddressDto address : items) {
            table.addCell(String.valueOf(address.getId()));
            table.addCell(safe(address.getLine1()));
            table.addCell(safe(address.getLine2()));
            table.addCell(safe(address.getPostalCode()));
            table.addCell(safe(address.getSuburbName()));
            table.addCell(safe(address.getDistrictName()));
            table.addCell(safe(address.getProvinceName()));
            table.addCell(safe(address.getCountryName()));
            table.addCell(address.getCreatedAt() != null ? address.getCreatedAt().toString() : "");
            table.addCell(address.getUpdatedAt() != null ? address.getUpdatedAt().toString() : "");
            table.addCell(address.getEntityStatus() != null ? address.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public ImportSummary importAddressFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<AddressCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(AddressCsvDto.class);

            CsvToBean<AddressCsvDto> csvToBean = new CsvToBeanBuilder<AddressCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<AddressCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (AddressCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getLine1() == null || row.getLine1().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing address line 1");
                        continue;
                    }

                    CreateAddressRequest request = new CreateAddressRequest();
                    request.setLine1(row.getLine1());
                    request.setLine2(row.getLine2());
                    request.setPostalCode(row.getPostalCode());
                    request.setSuburbId(row.getSuburbId());

                    AddressResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + rowNum + ": " + response.getMessage());
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowNum + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " addresses imported."
                : "Import failed. No addresses were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private AddressResponse buildAddressResponse(int statusCode, boolean isSuccess, String message,
                                               AddressDto addressDto, List<AddressDto> addressDtoList,
                                               Page<AddressDto> addressDtoPage) {

        AddressResponse addressResponse = new AddressResponse();

        addressResponse.setStatusCode(statusCode);
        addressResponse.setSuccess(isSuccess);
        addressResponse.setMessage(message);
        addressResponse.setAddressDto(addressDto);
        addressResponse.setAddressDtoList(addressDtoList);
        addressResponse.setAddressDtoPage(addressDtoPage);

        return addressResponse;
    }

    private AddressResponse buildAddressResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                         AddressDto addressDto, List<AddressDto> addressDtoList,
                                                         Page<AddressDto> addressDtoPage, List<String> errorMessages) {

        AddressResponse addressResponse = new AddressResponse();

        addressResponse.setStatusCode(statusCode);
        addressResponse.setSuccess(isSuccess);
        addressResponse.setMessage(message);
        addressResponse.setAddressDto(addressDto);
        addressResponse.setAddressDtoList(addressDtoList);
        addressResponse.setAddressDtoPage(addressDtoPage);
        addressResponse.setErrorMessages(errorMessages);

        return addressResponse;
    }
}
