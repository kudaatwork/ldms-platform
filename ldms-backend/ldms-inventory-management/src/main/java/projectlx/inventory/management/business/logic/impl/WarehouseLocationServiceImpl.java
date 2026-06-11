package projectlx.inventory.management.business.logic.impl;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import projectlx.inventory.management.business.auditable.api.WarehouseLocationServiceAuditable;
import projectlx.inventory.management.business.logic.api.WarehouseLocationService;
import projectlx.inventory.management.business.validator.api.WarehouseLocationServiceValidator;
import projectlx.inventory.management.clients.LocationsServiceClient;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseLocationType;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.WarehouseLocationSpecification;
import projectlx.inventory.management.utils.dtos.WarehouseLocationDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.WarehouseLocationMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.EditWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.CreateAddressRequest;
import projectlx.inventory.management.utils.responses.AddressResponse;
import projectlx.inventory.management.utils.responses.WarehouseLocationResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class WarehouseLocationServiceImpl implements WarehouseLocationService {

    private final WarehouseLocationServiceValidator validator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final WarehouseLocationRepository repository;
    private final WarehouseLocationServiceAuditable warehouseLocationServiceAuditable;
    private final LocationsServiceClient locationsServiceClient;

    private static final String[] HEADERS = {
            "ID", "LOCATION_ID", "SUPPLIER_ID", "CREATED_AT", "UPDATED_AT", "STATUS"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "DESCRIPTION", "LINE1", "LINE2", "POSTAL_CODE",
            "SUBURB_ID", "GEO_COORDINATES_ID", "SUPPLIER_ID", "WAREHOUSE_TYPE"
    };

    @Override
    public WarehouseLocationResponse create(CreateWarehouseLocationRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isCreateWarehouseLocationRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_WAREHOUSE_LOCATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        // Create address in Locations Service first
        CreateAddressRequest createAddressRequest = new CreateAddressRequest();
        createAddressRequest.setLine1(request.getLine1());
        createAddressRequest.setLine2(request.getLine2());
        createAddressRequest.setPostalCode(request.getPostalCode());
        createAddressRequest.setSuburbId(request.getSuburbId());
        createAddressRequest.setGeoCoordinatesId(request.getGeoCoordinatesId());

        try {

            AddressResponse addressResponse = locationsServiceClient.create(createAddressRequest, locale);

            if (addressResponse != null && addressResponse.isSuccess() && addressResponse.getAddressDto() != null) {

                WarehouseLocation wareHouseLocationToSave = new WarehouseLocation();
                wareHouseLocationToSave.setLocationId(String.valueOf(addressResponse.getAddressDto().getId()));
                wareHouseLocationToSave.setSupplierId(request.getSupplierId());
                wareHouseLocationToSave.setName(request.getName());
                wareHouseLocationToSave.setDescription(request.getDescription());
                wareHouseLocationToSave.setWarehouseType(request.getWarehouseType());

                WarehouseLocation wareHouseLocationSaved = warehouseLocationServiceAuditable.create(wareHouseLocationToSave, locale,
                        username);
                modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
                WarehouseLocationDto dto = modelMapper.map(wareHouseLocationSaved, WarehouseLocationDto.class);

                message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_CREATED_SUCCESSFULLY.getCode(),
                        new String[]{}, locale);
                return buildResponse(201, true, message, dto, null, null);

            } else {
                // If address creation failed, propagate error message if present
                message = (addressResponse != null && addressResponse.getMessage() != null)
                        ? addressResponse.getMessage()
                        : messageService.getMessage(I18Code.MESSAGE_CREATE_WAREHOUSE_LOCATION_INVALID_REQUEST.getCode(),
                        new String[]{}, locale);
                return buildResponse(400, false, message, null, null, null);
            }
        } catch (Exception ex) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_WAREHOUSE_LOCATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponse(500, false, message, null, null,
                    List.of(ex.getMessage()));
        }
    }

    @Override
    public WarehouseLocationResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<WarehouseLocation> retrieved = repository.findById(id);

        if (retrieved.isEmpty() || retrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        WarehouseLocationDto dto = modelMapper.map(retrieved.get(), WarehouseLocationDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public WarehouseLocationResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<WarehouseLocation> warehouseLocationList = repository.findAll();
        List<WarehouseLocation> filtered = new ArrayList<>();

        for (WarehouseLocation warehouseLocation : warehouseLocationList) {

            if (warehouseLocation.getEntityStatus() != EntityStatus.DELETED) {
                filtered.add(warehouseLocation);
            }
        }

        if (filtered.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        List<WarehouseLocationDto> list = modelMapper.map(filtered,
                new TypeToken<List<WarehouseLocationDto>>() {}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, null, list, null);
    }

    @Override
    public WarehouseLocationResponse update(EditWarehouseLocationRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_WAREHOUSE_LOCATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<WarehouseLocation> retrieved = repository.findById(request.getWarehouseLocationId());

        if (retrieved.isEmpty() || retrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(400, false, message, null, null, null);
        }

        WarehouseLocation toEdit = retrieved.get();

        if (request.getLocationId() != null) toEdit.setLocationId(request.getLocationId());
        if (request.getSupplierId() != null) toEdit.setSupplierId(request.getSupplierId());
        if (request.getWarehouseType() != null) toEdit.setWarehouseType(request.getWarehouseType());
        if (request.getName() != null) toEdit.setName(request.getName());
        if (request.getDescription() != null) toEdit.setDescription(request.getDescription());

        WarehouseLocation updated = warehouseLocationServiceAuditable.update(toEdit, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        WarehouseLocationDto dto = modelMapper.map(updated, WarehouseLocationDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public WarehouseLocationResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<WarehouseLocation> retrieved = repository.findById(id);

        if (retrieved.isEmpty() || retrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        WarehouseLocation toDelete = retrieved.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);
        WarehouseLocation deleted = warehouseLocationServiceAuditable.delete(toDelete, locale);

        WarehouseLocationDto warehouseLocationDto = modelMapper.map(deleted, WarehouseLocationDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_DELETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, warehouseLocationDto, null, null);
    }

    @Override
    public WarehouseLocationResponse findByMultipleFilters(WarehouseLocationMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<WarehouseLocation> spec = null;
        spec = addToSpec(spec, WarehouseLocationSpecification::deleted);

        // Validate the incoming request (must not be null and within bounds)
        ValidatorDto validatorDto = validator
                .isRequestValidToRetrieveWarehouseLocationByMultipleFilters(request, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message,
                    null, null, validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // Apply 'name' filter when valid
        ValidatorDto nameValidatorDto = validator.isStringValid(request.getName(), locale);

        if (nameValidatorDto.getSuccess()) {
            spec = addToSpec(request.getName(), spec, WarehouseLocationSpecification::nameLike);
        }

        // Apply 'description' filter when valid
        ValidatorDto descriptionValidatorDto = validator.isStringValid(request.getDescription(), locale);

        if (descriptionValidatorDto.getSuccess()) {
            spec = addToSpec(request.getName(), spec, WarehouseLocationSpecification::descriptionLike);
        }

        // Apply 'searchValue' filter when valid
        ValidatorDto searchValueValidatorDto = validator.isStringValid(request.getSearchValue(), locale);

        if (searchValueValidatorDto.getSuccess()) {
            spec = addToSpec(request.getSearchValue(), spec, WarehouseLocationSpecification::any);
        }

        // Page bounds check
        long totalCount = repository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Page<WarehouseLocation> result = repository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        // Map to DTO page
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<WarehouseLocationDto> dtoPage = result.map(pc ->
                modelMapper.map(pc, WarehouseLocationDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        WarehouseLocationResponse response = buildResponse(200, true, message,
                null, null, null);
        response.setWarehouseLocationDtoPage(dtoPage);

        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private Specification<WarehouseLocation> addToSpec(
            Specification<WarehouseLocation> spec,
            java.util.function.Function<EntityStatus, Specification<WarehouseLocation>> predicateMethod) {
        return spec == null ? predicateMethod.apply(EntityStatus.DELETED) : spec.and(predicateMethod.apply(EntityStatus.DELETED));
    }

    private Specification<WarehouseLocation> addToSpec(
            String aString,
            Specification<WarehouseLocation> spec,
            java.util.function.Function<String, Specification<WarehouseLocation>> predicateMethod) {
        if (aString == null || aString.trim().isEmpty()) return spec;
        String value = aString.toUpperCase();
        return spec == null ? predicateMethod.apply(value) : spec.and(predicateMethod.apply(value));
    }

    @Override
    public byte[] exportToCsv(List<WarehouseLocationDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (WarehouseLocationDto dto : items) {
            sb.append(dto.getId()).append(",")
              .append(safe(dto.getLocationId())).append(",")
              .append(dto.getSupplierId() == null ? "" : dto.getSupplierId()).append(",")
              .append(dto.getCreatedAt() == null ? "" : dto.getCreatedAt()).append(",")
              .append(dto.getUpdatedAt() == null ? "" : dto.getUpdatedAt()).append(",")
              .append(dto.getEntityStatus() == null ? "" : dto.getEntityStatus().name())
              .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<WarehouseLocationDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Warehouse Locations");

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (WarehouseLocationDto dto : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(dto.getId() == null ? 0 : dto.getId());
            row.createCell(1).setCellValue(safe(dto.getLocationId()));
            row.createCell(2).setCellValue(dto.getSupplierId() == null ? 0 : dto.getSupplierId());
            row.createCell(3).setCellValue(dto.getCreatedAt() == null ? "" : dto.getCreatedAt().toString());
            row.createCell(4).setCellValue(dto.getUpdatedAt() == null ? "" : dto.getUpdatedAt().toString());
            row.createCell(5).setCellValue(dto.getEntityStatus() == null ? "" : dto.getEntityStatus().name());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<WarehouseLocationDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (WarehouseLocationDto dto : items) {
            rows.add(new String[]{
                    String.valueOf(dto.getId() == null ? "" : dto.getId()),
                    safe(dto.getLocationId()),
                    String.valueOf(dto.getSupplierId() == null ? "" : dto.getSupplierId()),
                    dto.getCreatedAt() == null ? "" : dto.getCreatedAt().toString(),
                    dto.getUpdatedAt() == null ? "" : dto.getUpdatedAt().toString(),
                    dto.getEntityStatus() == null ? "" : dto.getEntityStatus().name()
            });
        }
        return InventoryExportSupport.writeTabularPdf("Warehouse Locations", "INV-WHL",
                "Warehouse location export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importWarehouseLocationFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> records = csvParser.getRecords();
            total = records.size();

            for (CSVRecord record : records) {
                try {
                    CreateWarehouseLocationRequest request = new CreateWarehouseLocationRequest();
                    request.setName(record.get("NAME"));
                    request.setDescription(record.get("DESCRIPTION"));
                    request.setLine1(record.get("LINE1"));
                    request.setLine2(record.get("LINE2"));
                    request.setPostalCode(record.get("POSTAL_CODE"));
                    request.setSuburbId(parseLongSafe(record, "SUBURB_ID"));
                    request.setGeoCoordinatesId(parseLongSafe(record, "GEO_COORDINATES_ID"));
                    request.setSupplierId(parseLongSafe(record, "SUPPLIER_ID"));
                    request.setWarehouseType(parseWarehouseTypeSafe(record.get("WAREHOUSE_TYPE")));

                    WarehouseLocationResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + record.getRecordNumber() + ": " + response.getMessage());
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + record.getRecordNumber() + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = isSuccess
                ? "Import completed successfully. " + success + " out of " + total + " locations imported."
                : "Import failed. No locations were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private Long parseLongSafe(CSVRecord record, String header) {
        try {
            String v = record.get(header);
            if (v == null || v.isBlank()) return null;
            return Long.parseLong(v.trim());
        } catch (Exception e) { return null; }
    }

    private WarehouseLocationType parseWarehouseTypeSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return WarehouseLocationType.SUPPLIER;
        }
        try {
            return WarehouseLocationType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return WarehouseLocationType.SUPPLIER;
        }
    }

    private WarehouseLocationResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                                    WarehouseLocationDto dto, List<WarehouseLocationDto> dtoList,
                                                    List<String> errorMessages) {
        WarehouseLocationResponse response = new WarehouseLocationResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setWarehouseLocationDto(dto);
        response.setWarehouseLocationDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private WarehouseLocationResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                              WarehouseLocationDto dto, List<WarehouseLocationDto> dtoList,
                                                              List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }
}
