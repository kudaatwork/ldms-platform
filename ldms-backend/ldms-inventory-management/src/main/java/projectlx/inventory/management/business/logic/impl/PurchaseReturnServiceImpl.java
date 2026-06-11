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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.PurchaseReturnServiceAuditable;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.logic.api.PurchaseReturnService;
import projectlx.inventory.management.business.validator.api.PurchaseReturnServiceValidator;
import projectlx.inventory.management.model.*;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.repository.PurchaseReturnRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.PurchaseReturnSpecification;
import projectlx.inventory.management.utils.dtos.PurchaseReturnDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreatePurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.NotificationRequest;
import projectlx.inventory.management.utils.requests.PurchaseReturnMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;
import projectlx.inventory.management.utils.responses.PurchaseReturnResponse;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseReturnServiceImpl implements PurchaseReturnService {

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PurchaseReturnServiceAuditable purchaseReturnServiceAuditable;
    private final InventoryItemService inventoryItemService;
    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final PurchaseReturnServiceValidator validator;
    private final RabbitTemplate rabbitTemplate;

    private static final String[] HEADERS = {"ID", "RETURN_NUMBER", "PURCHASE_ORDER_ID", "WAREHOUSE_LOCATION_ID",
            "RETURNED_BY_USER_ID", "REASON", "CREATED_AT", "UPDATED_AT"};
    private static final String[] CSV_HEADERS = {"PURCHASE_ORDER_ID", "WAREHOUSE_LOCATION_ID", "RETURNED_BY_USER_ID",
            "REASON"};


    @Override
    @Transactional
    public PurchaseReturnResponse create(CreatePurchaseReturnRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isCreatePurchaseReturnRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrder> purchaseOrderOpt = purchaseOrderRepository.findByIdAndEntityStatusNot(request.getPurchaseOrderId(), EntityStatus.DELETED);

        if (purchaseOrderOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Optional<WarehouseLocation> locationOpt = warehouseLocationRepository.findByIdAndEntityStatusNot(
                request.getWarehouseLocationId(), EntityStatus.DELETED);

        if (locationOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setPurchaseOrder(purchaseOrderOpt.get());
        purchaseReturn.setWarehouseLocation(locationOpt.get());
        purchaseReturn.setReturnedByUserId(request.getReturnedByUserId());
        purchaseReturn.setReason(request.getReason());
        purchaseReturn.setReturnNumber(generateReturnNumber());
        PurchaseReturn savedReturn = purchaseReturnServiceAuditable.create(purchaseReturn, locale, username);

        List<String> errors = new ArrayList<>();

        if (request.getReturnedLineItems() != null) {

            for (CreatePurchaseReturnRequest.ReturnedLineItem returnedItem : request.getReturnedLineItems()) {

                try {

                    Optional<InventoryItem> inventoryItemOpt =
                            inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(returnedItem.getProductId(),
                                    request.getWarehouseLocationId(), EntityStatus.DELETED);

                    if (inventoryItemOpt.isEmpty()) {
                        errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_INVENTORY_ITEM_NOT_FOUND.getCode(),
                                new String[]{returnedItem.getProductId().toString()}, locale));
                        continue;
                    }

                    InventoryItemResponse response = inventoryItemService.recordPurchaseReturn(inventoryItemOpt.get().getId(),
                            returnedItem.getQuantityReturned(), request.getReason(), request.getReturnedByUserId(),
                            savedReturn.getId(), ReferenceDocumentType.PURCHASE_RETURN, returnedItem.getUnitCost(),
                            locale, username);

                    if (!response.isSuccess()) {
                        errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_FAILED_TO_RECORD.getCode(),
                                new String[]{returnedItem.getProductId().toString(), response.getMessage()}, locale));
                    }

                } catch (Exception e) {
                    log.error("Error recording purchase return for line item.", e);
                    errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_UNEXPECTED_ERROR.getCode(),
                            new String[]{returnedItem.getProductId().toString(), e.getMessage()}, locale));
                }
            }
        }

        if (!errors.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_PARTIALLY_RECORDED.getCode(), new String[]{},
                    locale) + " " + String.join(", ", errors);

            return buildResponseWithErrors(400, false, message, null, null, errors);
        }

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_CREATED_SUCCESSFULLY.getCode(), new String[]{}, 
                locale);

        PurchaseReturnDto dto = modelMapper.map(savedReturn, PurchaseReturnDto.class);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public PurchaseReturnResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<PurchaseReturn> purchaseReturnOpt = purchaseReturnRepository.findByIdAndEntityStatusNot(id, 
                EntityStatus.DELETED);

        if (purchaseReturnOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseReturnDto dto = modelMapper.map(purchaseReturnOpt.get(), PurchaseReturnDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_RETRIEVED_SUCCESSFULLY.getCode(), 
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public Optional<PurchaseReturn> findById(Long id) {
        return purchaseReturnRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
    }

    @Override
    public PurchaseReturnResponse findAllAsList(Locale locale, String username) {

        String message = "";
        List<PurchaseReturn> purchaseReturnList = purchaseReturnRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if (purchaseReturnList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        List<PurchaseReturnDto> dtoList = purchaseReturnList.stream()
                .map(pr -> modelMapper.map(pr, PurchaseReturnDto.class))
                .collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_RETRIEVED_SUCCESSFULLY.getCode(), 
                new String[]{}, locale);

        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    @Transactional
    public PurchaseReturnResponse update(EditPurchaseReturnRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PURCHASE_RETURN_INVALID.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<PurchaseReturn> existingOpt = purchaseReturnRepository.findById(request.getPurchaseReturnId());

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseReturn toEdit = existingOpt.get();
        modelMapper.map(request, toEdit);
        toEdit.setUpdatedAt(LocalDateTime.now());

        PurchaseReturn saved = purchaseReturnServiceAuditable.update(toEdit, locale, username);
        PurchaseReturnDto dto = modelMapper.map(saved, PurchaseReturnDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, 
                locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    @Transactional
    public PurchaseReturnResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<PurchaseReturn> existingOpt = purchaseReturnRepository.findById(id);

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseReturn toDelete = existingOpt.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);
        PurchaseReturn deleted = purchaseReturnServiceAuditable.delete(toDelete, locale);

        PurchaseReturnDto purchaseReturnDto = modelMapper.map(deleted, PurchaseReturnDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildResponse(200, true, message, purchaseReturnDto, null, null);
    }

    @Override
    public PurchaseReturnResponse findByMultipleFilters(PurchaseReturnMultipleFiltersRequest request, String username, 
                                                        Locale locale) {
        
        String message = "";
        Specification<PurchaseReturn> spec = null;
        spec = addToSpec(spec, PurchaseReturnSpecification::deleted);

        ValidatorDto validatorDto = validator.isRequestValidToRetrievePurchaseReturnByMultipleFilters(request, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (request.getPurchaseReturnId() != null) {

            spec = (spec == null)
                    ? Specification.where((root, query, cb) ->
                    cb.equal(root.get("id"), request.getPurchaseReturnId()))
                    : spec.and((root, query, cb) -> cb.equal(
                            root.get("id"), request.getPurchaseReturnId()));
        }

        if (request.getPurchaseOrderId() != null) {

            spec = (spec == null)
                    ? PurchaseReturnSpecification.purchaseOrderIdEquals(request.getPurchaseOrderId())
                    : spec.and(PurchaseReturnSpecification.purchaseOrderIdEquals(request.getPurchaseOrderId()));
        }

        if (request.getWarehouseLocationId() != null) {

            spec = (spec == null)
                    ? PurchaseReturnSpecification.warehouseLocationIdEquals(request.getWarehouseLocationId())
                    : spec.and(PurchaseReturnSpecification.warehouseLocationIdEquals(request.getWarehouseLocationId()));
        }

        if (request.getReturnedByUserId() != null) {

            spec = (spec == null)
                    ? PurchaseReturnSpecification.returnedByUserIdEquals(request.getReturnedByUserId())
                    : spec.and(PurchaseReturnSpecification.returnedByUserIdEquals(request.getReturnedByUserId()));
        }

        ValidatorDto returnNumberValidatorDto = validator.isStringValid(request.getReturnNumber(), locale);

        if (returnNumberValidatorDto.getSuccess()) {

            spec = addToSpec(request.getReturnNumber(), spec, PurchaseReturnSpecification::returnNumberLike);
        }

        if (request.getEntityStatus() != null) {

            spec = (spec == null)
                    ? PurchaseReturnSpecification.entityStatusEquals(request.getEntityStatus())
                    : spec.and(PurchaseReturnSpecification.entityStatusEquals(request.getEntityStatus()));
        }

        long totalCount = purchaseReturnRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage && totalCount > 0) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Page<PurchaseReturn> result = purchaseReturnRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<PurchaseReturnDto> purchaseReturnDtoPage = result.map(item -> modelMapper.map(item,
                PurchaseReturnDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        PurchaseReturnResponse response = buildResponse(200, true, message, null, null,
                null);

        response.setPurchaseReturnDtoPage(purchaseReturnDtoPage);
        return response;
    }

    private Specification<PurchaseReturn> addToSpec(Specification<PurchaseReturn> spec, Function<EntityStatus, Specification<PurchaseReturn>> predicateMethod) {
        return spec == null ? predicateMethod.apply(EntityStatus.DELETED) : spec.and(predicateMethod.apply(EntityStatus.DELETED));
    }

    private Specification<PurchaseReturn> addToSpec(
            String aString,
            Specification<PurchaseReturn> spec,
            Function<String, Specification<PurchaseReturn>> predicateMethod) {
        if (aString == null || aString.trim().isEmpty()) return spec;
        String value = aString.toUpperCase();
        return spec == null ? predicateMethod.apply(value) : spec.and(predicateMethod.apply(value));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<PurchaseReturnDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (PurchaseReturnDto item : items) {
            sb.append(item.getId() != null ? item.getId() : "").append(",")
                    .append(safe(item.getReturnNumber())).append(",")
                    .append(item.getPurchaseOrderId() != null ? item.getPurchaseOrderId() : "").append(",")
                    .append(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : "").append(",")
                    .append(item.getReturnedByUserId() != null ? item.getReturnedByUserId() : "").append(",")
                    .append(safe(item.getReason())).append(",")
                    .append(item.getCreatedAt() != null ? item.getCreatedAt().toString() : "").append(",")
                    .append(item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : "").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<PurchaseReturnDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Purchase Returns");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
        int rowIdx = 1;
        for (PurchaseReturnDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(safe(item.getReturnNumber()));
            row.createCell(2).setCellValue(item.getPurchaseOrderId() != null ? item.getPurchaseOrderId() : 0);
            row.createCell(3).setCellValue(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : 0);
            row.createCell(4).setCellValue(item.getReturnedByUserId() != null ? item.getReturnedByUserId() : 0);
            row.createCell(5).setCellValue(safe(item.getReason()));
            row.createCell(6).setCellValue(item.getCreatedAt() != null ? item.getCreatedAt().toString() : "");
            row.createCell(7).setCellValue(item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<PurchaseReturnDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (PurchaseReturnDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    safe(item.getReturnNumber()),
                    String.valueOf(item.getPurchaseOrderId() != null ? item.getPurchaseOrderId() : 0),
                    String.valueOf(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : 0),
                    String.valueOf(item.getReturnedByUserId() != null ? item.getReturnedByUserId() : 0),
                    safe(item.getReason()),
                    item.getCreatedAt() != null ? item.getCreatedAt().toString() : "",
                    item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf("Purchase Returns", "INV-PRN",
                "Purchase return export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importPurchaseReturnFromCsv(InputStream csvInputStream) throws IOException {

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
                    CreatePurchaseReturnRequest request = new CreatePurchaseReturnRequest();
                    request.setPurchaseOrderId(record.isMapped("PURCHASE_ORDER_ID") && !record.get("PURCHASE_ORDER_ID").isBlank() ? Long.parseLong(record.get("PURCHASE_ORDER_ID").trim()) : null);
                    request.setWarehouseLocationId(record.isMapped("WAREHOUSE_LOCATION_ID") && !record.get("WAREHOUSE_LOCATION_ID").isBlank() ? Long.parseLong(record.get("WAREHOUSE_LOCATION_ID").trim()) : null);
                    request.setReturnedByUserId(record.isMapped("RETURNED_BY_USER_ID") && !record.get("RETURNED_BY_USER_ID").isBlank() ? Long.parseLong(record.get("RETURNED_BY_USER_ID").trim()) : null);
                    request.setReason(record.isMapped("REASON") ? record.get("REASON") : null);

                    PurchaseReturnResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_IMPORT_ROW_ERROR.getCode(),
                                new String[]{String.valueOf(record.getRecordNumber()), response.getMessage()}, Locale.ENGLISH));
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_IMPORT_UNEXPECTED_ERROR.getCode(),
                            new String[]{String.valueOf(record.getRecordNumber()), e.getMessage()}, Locale.ENGLISH));
                }
            }
        }

        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;

        String message = isSuccess
                ? messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_IMPORT_SUCCESS.getCode(),
                        new String[]{String.valueOf(success), String.valueOf(total)}, Locale.ENGLISH)
                : messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_IMPORT_FAILED.getCode(),
                        new String[]{}, Locale.ENGLISH);
        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private String generateReturnNumber() {
        return "PR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private PurchaseReturnResponse buildResponse(int statusCode, boolean isSuccess, String message, PurchaseReturnDto dto, List<PurchaseReturnDto> dtoList, List<String> errorMessages) {
        PurchaseReturnResponse response = new PurchaseReturnResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setPurchaseReturnDto(dto);
        response.setPurchaseReturnDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private PurchaseReturnResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message, PurchaseReturnDto dto, List<PurchaseReturnDto> dtoList, List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }

    private void sendPurchaseReturnCreatedInternal(PurchaseReturn purchaseReturn) {
        try {
            Map<String, Object> internalData = Map.of(
                    "returnNumber", purchaseReturn.getReturnNumber(),
                    "purchaseOrderId", purchaseReturn.getPurchaseOrder() != null ? purchaseReturn.getPurchaseOrder().getId() : null,
                    "warehouseLocationId", purchaseReturn.getWarehouseLocation() != null ? purchaseReturn.getWarehouseLocation().getId() : null,
                    "returnedByUserId", purchaseReturn.getReturnedByUserId(),
                    "reason", purchaseReturn.getReason()
            );

            NotificationRequest.Recipient internalRecipient = new NotificationRequest.Recipient(
                    null, null, null, null
            );

            NotificationRequest notification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "PURCHASE_RETURN_CREATED_INTERNAL_EMAIL",
                    internalRecipient,
                    internalData,
                    null
            );

            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notification);
            log.info("Sent purchase return created internal notification for return {}", purchaseReturn.getReturnNumber());
        } catch (Exception e) {
            log.error("Failed to send purchase return created internal notification for return {}. Error: {}",
                    purchaseReturn.getReturnNumber(), e.getMessage());
        }
    }
}
