package projectlx.inventory.management.business.logic.impl;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.SalesReservationServiceAuditable;
import projectlx.inventory.management.business.logic.api.SalesReservationService;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.validator.api.SalesReservationServiceValidator;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.SalesReservationRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.SalesReservationSpecification;
import projectlx.inventory.management.utils.dtos.SalesReservationDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateSalesReservationRequest;
import projectlx.inventory.management.utils.requests.EditSalesReservationRequest;
import projectlx.inventory.management.utils.requests.SalesReservationMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesReservationResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class SalesReservationServiceImpl implements SalesReservationService {

    private final SalesReservationRepository salesReservationRepository;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SalesReservationServiceAuditable salesReservationServiceAuditable;
    private final InventoryItemService inventoryItemService;
    private final SalesReservationServiceValidator validator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    private static final String[] HEADERS = {"ID", "RESERVATION_NUMBER", "CUSTOMER_ID", "PRODUCT_ID",
            "WAREHOUSE_LOCATION_ID", "QUANTITY_RESERVED", "RESERVED_UNTIL", "STATUS", "CREATED_AT"};
    private static final String[] CSV_HEADERS = {"CUSTOMER_ID", "PRODUCT_ID", "WAREHOUSE_LOCATION_ID",
            "QUANTITY_RESERVED", "RESERVED_UNTIL"};

    private static final String RESERVATION_NUMBER_PREFIX = "SR-";
    private static final String IMPORT_USERNAME = "IMPORT_SCRIPT";

    @Override
    @Transactional
    public SalesReservationResponse create(CreateSalesReservationRequest request, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isCreateSalesReservationRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_SALES_RESERVATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        // Validate product exists
        Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(request.getProductId(),
                EntityStatus.DELETED);

        if (productOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        // Validate warehouse location exists
        Optional<WarehouseLocation> warehouseLocationOpt = warehouseLocationRepository
                .findByIdAndEntityStatusNot(request.getWarehouseLocationId(), EntityStatus.DELETED);

        if (warehouseLocationOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        // Check inventory availability
        Optional<InventoryItem> inventoryItemOpt = inventoryItemService
                .findInventoryItemByProductIdAndWarehouseId(request.getProductId(), request.getWarehouseLocationId());

        if (inventoryItemOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        InventoryItem inventoryItem = inventoryItemOpt.get();
        BigDecimal availableQuantity = getAvailableQuantity(inventoryItem);

        if (availableQuantity.compareTo(request.getQuantityReserved()) < 0) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_INSUFFICIENT_STOCK.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        // Create reservation
        SalesReservation salesReservation = new SalesReservation();
        salesReservation.setReservationNumber(generateReservationNumber());
        salesReservation.setCustomerId(request.getCustomerId());
        salesReservation.setProduct(productOpt.get());
        salesReservation.setWarehouseLocation(warehouseLocationOpt.get());
        salesReservation.setQuantityReserved(request.getQuantityReserved());
        salesReservation.setReservedUntil(request.getReservedUntil());
        salesReservation.setReservationStatus(ReservationStatus.ACTIVE);
        salesReservation.setCreatedByUserId(request.getCreatedByUserId());
        salesReservation.setNotes(request.getNotes());

        // Update inventory reservation
        BigDecimal currentReserved = inventoryItem.getReservedQuantity() != null ?
                inventoryItem.getReservedQuantity() : BigDecimal.ZERO;
        inventoryItem.setReservedQuantity(currentReserved.add(request.getQuantityReserved()));

        SalesReservation saved = salesReservationServiceAuditable.create(salesReservation, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesReservationDto dto = modelMapper.map(saved, SalesReservationDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public SalesReservationResponse findById(Long id, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<SalesReservation> salesReservationOpt = salesReservationRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (salesReservationOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesReservationDto dto = modelMapper.map(salesReservationOpt.get(), SalesReservationDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public SalesReservationResponse findAllAsList(Locale locale, String username) {

        String message;

        List<SalesReservation> salesReservationList = salesReservationRepository
                .findByEntityStatusNot(EntityStatus.DELETED);

        if (salesReservationList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<SalesReservationDto> dtoList = salesReservationList.stream()
                .map(reservation -> modelMapper.map(reservation, SalesReservationDto.class))
                .collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    @Transactional
    public SalesReservationResponse update(EditSalesReservationRequest request, String username, Locale locale) {

        String message;

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_SALES_RESERVATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<SalesReservation> existingOpt = salesReservationRepository
                .findByIdAndEntityStatusNot(request.getSalesReservationId(), EntityStatus.DELETED);

        if (existingOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        SalesReservation salesReservation = existingOpt.get();

        // Cannot update expired or cancelled reservations
        if (salesReservation.getReservationStatus() == ReservationStatus.EXPIRED ||
                salesReservation.getReservationStatus() == ReservationStatus.CANCELLED) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_NOT_EDITABLE.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        // Handle quantity changes
        if (request.getQuantityReserved() != null &&
                !request.getQuantityReserved().equals(salesReservation.getQuantityReserved())) {

            BigDecimal quantityDifference = request.getQuantityReserved()
                    .subtract(salesReservation.getQuantityReserved());

            Optional<InventoryItem> inventoryItemOpt = inventoryItemService
                    .findInventoryItemByProductIdAndWarehouseId(salesReservation.getProduct().getId(),
                            salesReservation.getWarehouseLocation().getId());

            if (inventoryItemOpt.isPresent()) {
                InventoryItem inventoryItem = inventoryItemOpt.get();

                if (quantityDifference.compareTo(BigDecimal.ZERO) > 0) {
                    // Increasing reservation - check availability
                    BigDecimal availableQuantity = getAvailableQuantity(inventoryItem);
                    if (availableQuantity.compareTo(quantityDifference) < 0) {
                        message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_INSUFFICIENT_STOCK.getCode(),
                                new String[]{}, locale);
                        return buildResponse(400, false, message, null, null, null);
                    }
                }

                // Update reservation quantity
                BigDecimal currentReserved = inventoryItem.getReservedQuantity() != null ?
                        inventoryItem.getReservedQuantity() : BigDecimal.ZERO;
                inventoryItem.setReservedQuantity(currentReserved.add(quantityDifference));
            }

            salesReservation.setQuantityReserved(request.getQuantityReserved());
        }

        // Update other fields
        if (request.getReservedUntil() != null) {
            salesReservation.setReservedUntil(request.getReservedUntil());
        }

        if (request.getReservationStatus() != null) {
            updateReservationStatus(salesReservation, request.getReservationStatus(), locale, username);
        }

        if (request.getNotes() != null) {
            salesReservation.setNotes(request.getNotes());
        }

        salesReservation.setUpdatedByUserId(request.getUpdatedByUserId());

        SalesReservation saved = salesReservationServiceAuditable.update(salesReservation, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesReservationDto dto = modelMapper.map(saved, SalesReservationDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    @Transactional
    public SalesReservationResponse delete(Long id, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<SalesReservation> existingOpt = salesReservationRepository.findById(id);

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        SalesReservation salesReservation = existingOpt.get();

        // Release inventory reservation if still active
        if (salesReservation.getReservationStatus() == ReservationStatus.ACTIVE) {
            releaseInventoryReservation(salesReservation);
        }

        salesReservation.setEntityStatus(EntityStatus.DELETED);
        salesReservation.setReservationStatus(ReservationStatus.CANCELLED);

        SalesReservation saved = salesReservationServiceAuditable.delete(salesReservation, locale);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesReservationDto dto = modelMapper.map(saved, SalesReservationDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_DELETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public SalesReservationResponse findByMultipleFilters(SalesReservationMultipleFiltersRequest request,
                                                          String username, Locale locale) {

        String message = "";

        Specification<SalesReservation> spec = SalesReservationSpecification.deleted();

        ValidatorDto validatorDto = validator.isRequestValidToRetrieveSalesReservationByMultipleFilters(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // Apply filters
        if (request.getCustomerId() != null) {
            spec = spec.and(SalesReservationSpecification.customerIdEquals(request.getCustomerId()));
        }

        if (request.getProductId() != null) {
            spec = spec.and(SalesReservationSpecification.productIdEquals(request.getProductId()));
        }

        if (request.getWarehouseLocationId() != null) {
            spec = spec.and(SalesReservationSpecification.warehouseLocationIdEquals(request.getWarehouseLocationId()));
        }

        if (request.getReservationStatus() != null) {
            spec = spec.and(SalesReservationSpecification.reservationStatusEquals(request.getReservationStatus()));
        }

        if (request.getSearchValue() != null && !request.getSearchValue().isBlank()) {
            spec = spec.and(SalesReservationSpecification.any(request.getSearchValue()));
        }

        long totalCount = salesReservationRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage && totalCount > 0) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Page<SalesReservation> result = salesReservationRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<SalesReservationDto> dtoPage = result.map(reservation ->
                modelMapper.map(reservation, SalesReservationDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_SALES_RESERVATION_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        SalesReservationResponse response = buildResponse(200, true, message, null, null, null);
        response.setSalesReservationDtoPage(dtoPage);
        return response;
    }

    @Override
    public byte[] exportToCsv(List<SalesReservationDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (SalesReservationDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(safe(item.getReservationNumber())).append(",")
                    .append(item.getCustomerId() != null ? item.getCustomerId() : "").append(",")
                    .append(item.getProductId() != null ? item.getProductId() : "").append(",")
                    .append(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : "").append(",")
                    .append(item.getQuantityReserved() != null ? item.getQuantityReserved() : "").append(",")
                    .append(item.getReservedUntil() != null ? item.getReservedUntil() : "").append(",")
                    .append(item.getReservationStatus() != null ? item.getReservationStatus().name() : "").append(",")
                    .append(item.getCreatedAt() != null ? item.getCreatedAt() : "").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<SalesReservationDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales Reservations");
        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (SalesReservationDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(safe(item.getReservationNumber()));
            row.createCell(2).setCellValue(item.getCustomerId() != null ? item.getCustomerId() : 0);
            row.createCell(3).setCellValue(item.getProductId() != null ? item.getProductId() : 0);
            row.createCell(4).setCellValue(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : 0);
            row.createCell(5).setCellValue(item.getQuantityReserved() != null ? item.getQuantityReserved().doubleValue() : 0);
            row.createCell(6).setCellValue(item.getReservedUntil() != null ? item.getReservedUntil().toString() : "");
            row.createCell(7).setCellValue(item.getReservationStatus() != null ? item.getReservationStatus().name() : "");
            row.createCell(8).setCellValue(item.getCreatedAt() != null ? item.getCreatedAt().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<SalesReservationDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (SalesReservationDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    safe(item.getReservationNumber()),
                    String.valueOf(item.getCustomerId() != null ? item.getCustomerId() : 0),
                    String.valueOf(item.getProductId() != null ? item.getProductId() : 0),
                    String.valueOf(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : 0),
                    String.valueOf(item.getQuantityReserved() != null ? item.getQuantityReserved() : 0),
                    item.getReservedUntil() != null ? item.getReservedUntil().toString() : "",
                    item.getReservationStatus() != null ? item.getReservationStatus().name() : "",
                    item.getCreatedAt() != null ? item.getCreatedAt().toString() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf("Sales Reservations", "INV-SRS",
                "Sales reservation export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importSalesReservationFromCsv(InputStream csvInputStream) throws IOException {

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
                    CreateSalesReservationRequest request = new CreateSalesReservationRequest();
                    request.setCustomerId(record.isMapped("CUSTOMER_ID") && !record.get("CUSTOMER_ID").isBlank() ?
                            Long.parseLong(record.get("CUSTOMER_ID").trim()) : null);
                    request.setProductId(record.isMapped("PRODUCT_ID") && !record.get("PRODUCT_ID").isBlank() ?
                            Long.parseLong(record.get("PRODUCT_ID").trim()) : null);
                    request.setWarehouseLocationId(record.isMapped("WAREHOUSE_LOCATION_ID") &&
                            !record.get("WAREHOUSE_LOCATION_ID").isBlank() ?
                            Long.parseLong(record.get("WAREHOUSE_LOCATION_ID").trim()) : null);
                    request.setQuantityReserved(record.isMapped("QUANTITY_RESERVED") &&
                            !record.get("QUANTITY_RESERVED").isBlank() ?
                            new BigDecimal(record.get("QUANTITY_RESERVED").trim()) : null);
                    request.setReservedUntil(record.isMapped("RESERVED_UNTIL") &&
                            !record.get("RESERVED_UNTIL").isBlank() ?
                            LocalDateTime.parse(record.get("RESERVED_UNTIL").trim()) : null);

                    SalesReservationResponse response = create(request, Locale.ENGLISH, IMPORT_USERNAME);
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
                ? "Import completed successfully. " + success + " out of " + total + " sales reservations imported."
                : "Import failed. No sales reservations were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    // Helper methods

    private String generateReservationNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String millis = String.valueOf(System.currentTimeMillis());
        String suffix = millis.substring(millis.length() - 4);
        return RESERVATION_NUMBER_PREFIX + datePart + "-" + suffix;
    }

    private void updateReservationStatus(SalesReservation salesReservation, ReservationStatus newStatus,
                                         Locale locale, String username) {

        ReservationStatus currentStatus = salesReservation.getReservationStatus();

        if (currentStatus == ReservationStatus.ACTIVE &&
                (newStatus == ReservationStatus.CANCELLED || newStatus == ReservationStatus.EXPIRED ||
                        newStatus == ReservationStatus.FULFILLED)) {
            // Release inventory reservation
            releaseInventoryReservation(salesReservation);
        }

        salesReservation.setReservationStatus(newStatus);
    }

    private void releaseInventoryReservation(SalesReservation salesReservation) {
        Product product = salesReservation.getProduct();
        if (product != null) {
            // Note: This logic may need to be updated based on how Product handles reservations
            // The original logic was for InventoryItem, but now we're using Product
            // This may require updating the Product entity or using a different approach
            // For now, commenting out the problematic line until the Product reservation logic is defined
            // product.releaseReservedQuantity(salesReservation.getQuantityReserved());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private SalesReservationResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                                   SalesReservationDto dto, List<SalesReservationDto> dtoList,
                                                   List<String> errorMessages) {
        SalesReservationResponse response = new SalesReservationResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setSalesReservationDto(dto);
        response.setSalesReservationDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private SalesReservationResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                             SalesReservationDto dto, List<SalesReservationDto> dtoList,
                                                             List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }

    /**
     * Calculate available quantity for reservation
     * Available = Current Stock - Reserved Quantity
     *
     * @param inventoryItem the inventory item to check
     * @return the available quantity that can be reserved
     */
    private BigDecimal getAvailableQuantity(InventoryItem inventoryItem) {

        BigDecimal currentQuantity = inventoryItem.getCurrentStock() != null ?
                inventoryItem.getCurrentStock() : BigDecimal.ZERO;

        BigDecimal reservedQuantity = inventoryItem.getReservedQuantity() != null ?
                inventoryItem.getReservedQuantity() : BigDecimal.ZERO;

        BigDecimal available = currentQuantity.subtract(reservedQuantity);

        // Ensure we don't return negative available quantity
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }
}