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
import projectlx.inventory.management.business.auditable.api.InventoryItemServiceAuditable;
import projectlx.inventory.management.business.auditable.api.StockAdjustmentServiceAuditable;
import projectlx.inventory.management.business.auditable.api.StockTransactionHistoryServiceAuditable;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.logic.api.StockAdjustmentService;
import projectlx.inventory.management.business.validator.api.StockAdjustmentServiceValidator;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.StockAdjustment;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.model.TransactionType;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.StockAdjustmentRepository;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import projectlx.inventory.management.repository.specification.StockAdjustmentSpecification;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.StockAdjustmentDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateOrUpdateStockRequest;
import projectlx.inventory.management.utils.requests.CreateStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.EditStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.NotificationRequest;
import projectlx.inventory.management.utils.requests.StockAdjustmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.StockAdjustmentResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

@Transactional
@RequiredArgsConstructor
@Slf4j
public class StockAdjustmentServiceImpl implements StockAdjustmentService {

    private final StockAdjustmentRepository repository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockAdjustmentServiceAuditable auditable;
    private final StockAdjustmentServiceValidator validator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final InventoryItemService inventoryItemService;
    private final StockTransactionHistoryServiceAuditable stockTransactionHistoryServiceAuditable;
    private final RabbitTemplate rabbitTemplate;

    private static final String[] HEADERS = {"ID", "INVENTORY_ITEM_ID", "QUANTITY_DELTA", "REASON", "ADJUSTED_AT"};
    private static final String[] CSV_HEADERS = {"INVENTORY_ITEM_ID", "QUANTITY_DELTA", "REASON"};

    @Override
    @Transactional
    public StockAdjustmentResponse create(CreateStockAdjustmentRequest request, Locale locale, String username) {
        String message;

        ValidatorDto validatorDto = validator
                .isCreateStockAdjustmentRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_STOCK_ADJUSTMENT_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        // CRITICAL: InventoryItem MUST exist
        Optional<InventoryItem> itemOpt = inventoryItemRepository
                .findByIdAndEntityStatusNot(request.getInventoryItemId(), EntityStatus.DELETED);
        if (itemOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryItem inventoryItem = itemOpt.get();
        BigDecimal quantityDelta = request.getQuantityDelta();

        // Validate adjustment won't result in negative stock
        if (inventoryItem.getCurrentStock().add(quantityDelta).compareTo(BigDecimal.ZERO) < 0) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_STOCK_ADJUSTMENT_INSUFFICIENT_STOCK.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // Determine unit cost
        BigDecimal unitCostToUse;
        if (request.getUnitCost() != null && request.getUnitCost().compareTo(BigDecimal.ZERO) > 0) {
            unitCostToUse = request.getUnitCost();
        } else if (inventoryItem.getAverageCost() != null &&
                inventoryItem.getAverageCost().compareTo(BigDecimal.ZERO) > 0) {
            unitCostToUse = inventoryItem.getAverageCost();
        } else {
            unitCostToUse = BigDecimal.ZERO;
        }

        // Create adjustment record
        StockAdjustment stockAdjustment = new StockAdjustment();
        stockAdjustment.setInventoryItem(inventoryItem);
        stockAdjustment.setQuantityDelta(quantityDelta);
        stockAdjustment.setUnitCost(unitCostToUse);
        stockAdjustment.setReason(request.getReason() != null ? request.getReason().toUpperCase() : "ADJUSTMENT");
        stockAdjustment.setAdjustedByUserId(request.getAdjustedByUserId());

        StockAdjustment savedAdjustment = auditable.create(stockAdjustment, locale, username);

        // Update inventory with WAC method
        updateInventoryWithWAC(inventoryItem, quantityDelta, unitCostToUse);

        // Create transaction history
        StockTransactionHistory transaction = new StockTransactionHistory();
        transaction.setInventoryItem(inventoryItem);
        transaction.setTransactionType(quantityDelta.compareTo(BigDecimal.ZERO) >= 0 ?
                TransactionType.STOCK_IN : TransactionType.STOCK_OUT);
        transaction.setQuantityChange(quantityDelta);
        transaction.setUnitCost(unitCostToUse);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setWarehouseLocation(inventoryItem.getWarehouseLocation());
        transaction.setPerformedByUserId(request.getAdjustedByUserId());
        transaction.setReferenceDocumentId(savedAdjustment.getId());
        transaction.setReferenceDocumentType(ReferenceDocumentType.STOCK_ADJUSTMENT);
        transaction.setReason(request.getReason());

        stockTransactionHistoryServiceAuditable.create(transaction, locale, username);

        StockAdjustmentDto dto = modelMapper.map(savedAdjustment, StockAdjustmentDto.class);
        message = messageService.getMessage(
                I18Code.MESSAGE_STOCK_ADJUSTMENT_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        log.info("Stock adjustment created: Item={}, Qty={}, UnitCost={}, NewStock={}, NewAvgCost={}",
                inventoryItem.getId(), quantityDelta, unitCostToUse,
                inventoryItem.getCurrentStock(), inventoryItem.getAverageCost());

        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public StockAdjustmentResponse findById(Long id, Locale locale, String username) {
        
        String message;
        
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
       
        if (!validatorDto.getSuccess()) {
           
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
           
            return buildResponse(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Optional<StockAdjustment> adjustmentOpt = repository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
       
        if (adjustmentOpt.isEmpty()) {
          
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
          
            return buildResponse(404, false, message, null, null, null);
        }

        StockAdjustmentDto dto = modelMapper.map(adjustmentOpt.get(), StockAdjustmentDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_RETRIEVED_SUCCESSFULLY.getCode(), 
                new String[]{}, locale);
        
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public StockAdjustmentResponse findAllAsList(Locale locale, String username) {
       
        String message;
      
        List<StockAdjustment> list = repository.findByEntityStatusNot(EntityStatus.DELETED);
      
        if (list.isEmpty()) {
           
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
         
            return buildResponse(404, false, message, null, null, null);
        }

        List<StockAdjustmentDto> dtoList = list.stream().map(stockAdjustment -> modelMapper.map(stockAdjustment, 
                StockAdjustmentDto.class)).collect(Collectors.toList());
        
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_RETRIEVED_SUCCESSFULLY.getCode(), 
                new String[]{}, locale);
        
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse update(EditStockAdjustmentRequest request, String username, Locale locale) {
        
        String message;
        
        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);
        
        if (!validatorDto.getSuccess()) {
           
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_UPDATE_INVALID.getCode(), new String[]{}, 
                    locale);
          
            return buildResponseWithErrors(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Optional<StockAdjustment> existingOpt = repository.findById(request.getStockAdjustmentId());
        
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
        
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
        
            return buildResponse(404, false, message, null, null, null);
        }

        StockAdjustment toEdit = existingOpt.get();
        // Only allow updating non-critical fields
        if (request.getReason() != null) toEdit.setReason(request.getReason());
        if (request.getAdjustedByUserId() != null) toEdit.setAdjustedByUserId(request.getAdjustedByUserId());
        if (request.getEntityStatus() != null) toEdit.setEntityStatus(request.getEntityStatus());

        StockAdjustment saved = auditable.update(toEdit, locale, username);
        StockAdjustmentDto dto = modelMapper.map(saved, StockAdjustmentDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, 
                locale);
        
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse delete(Long id, Locale locale, String username) {
       
        String message;
       
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
      
        if (!validatorDto.getSuccess()) {
      
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
       
            return buildResponse(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Optional<StockAdjustment> existingOpt = repository.findById(id);
       
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
        
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_NOT_FOUND.getCode(), new String[]{}, 
                    locale);
        
            return buildResponse(404, false, message, null, null, null);
        }

        StockAdjustment toDelete = existingOpt.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);

        // Create an offsetting transaction to reverse the original stock change
        BigDecimal reversedDelta = toDelete.getQuantityDelta().negate();

        CreateOrUpdateStockRequest reversalRequest = new CreateOrUpdateStockRequest();
        reversalRequest.setProductId(toDelete.getInventoryItem().getProduct().getId());
        reversalRequest.setWarehouseLocationId(toDelete.getInventoryItem().getWarehouseLocation().getId());
        reversalRequest.setQuantityReceived(reversedDelta);
        reversalRequest.setReason("Reversal of adjustment ID " + toDelete.getId());
        reversalRequest.setUserId(toDelete.getAdjustedByUserId());
        reversalRequest.setReferenceDocumentId(toDelete.getId());
        reversalRequest.setReferenceDocumentType(ReferenceDocumentType.STOCK_ADJUSTMENT);
        reversalRequest.setUnitCost(BigDecimal.ZERO);

        inventoryItemService.createOrUpdateStock(reversalRequest, locale, username);

        StockAdjustment saved = auditable.delete(toDelete, locale);
        StockAdjustmentDto stockAdjustmentDto = modelMapper.map(saved, StockAdjustmentDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_DELETED_SUCCESSFULLY.getCode(), new String[]{}, 
                locale);
        
        return buildResponse(200, true, message, stockAdjustmentDto, null, null);
    }

    @Override
    public StockAdjustmentResponse findByMultipleFilters(StockAdjustmentMultipleFiltersRequest request, String username, Locale locale) {
       
        String message = "";
     
        Specification<StockAdjustment> spec = StockAdjustmentSpecification.deleted();

        ValidatorDto validatorDto = validator.isRequestValidToRetrieveStockAdjustmentByMultipleFilters(request, locale);
        
        if (!validatorDto.getSuccess()) {
           
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), 
                    new String[]{}, locale);
          
            return buildResponseWithErrors(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (request.getReason() != null && !request.getReason().isBlank()) {
            spec = spec.and(StockAdjustmentSpecification.reasonLike(request.getReason()));
        }

        if (request.getEntityStatus() != null) {
            spec = spec.and(StockAdjustmentSpecification.entityStatusEquals(request.getEntityStatus()));
        }
        if (request.getSearchValue() != null && !request.getSearchValue().isBlank()) {
            spec = spec.and(StockAdjustmentSpecification.any(request.getSearchValue()));
        }

        long totalCount = repository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());
        
        if (request.getPage() >= maxPage && totalCount > 0) {
           
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, 
                    locale);
         
            return buildResponse(404, false, message, null, null, null);
        }

        Page<StockAdjustment> result = repository.findAll(spec, pageable);
       
        if (result.getContent().isEmpty()) {
         
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_NOT_FOUND.getCode(), new String[]{}, locale);
          
            return buildResponse(404, false, message, null, null, null);
        }

        Page<StockAdjustmentDto> dtoPage = result.map(sa -> modelMapper.map(sa, StockAdjustmentDto.class));
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, 
                locale);
        
        StockAdjustmentResponse response = buildResponse(200, true, message, null, null, null);
        response.setStockAdjustmentDtoPage(dtoPage);
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<StockAdjustmentDto> items) {
       
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (StockAdjustmentDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(item.getInventoryItemId()).append(",")
                    .append(item.getQuantityDelta()).append(",")
                    .append(safe(item.getReason())).append(",")
                    .append(item.getAdjustedAt()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<StockAdjustmentDto> items) throws IOException {
       
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock Adjustments");
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
        int rowIdx = 1;
        for (StockAdjustmentDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(item.getInventoryItemId() != null ? item.getInventoryItemId() : 0);
            row.createCell(2).setCellValue(item.getQuantityDelta() != null ? item.getQuantityDelta().doubleValue() : 0);
            row.createCell(3).setCellValue(safe(item.getReason()));
            row.createCell(4).setCellValue(item.getAdjustedAt() != null ? item.getAdjustedAt().toString() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<StockAdjustmentDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (StockAdjustmentDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    String.valueOf(item.getInventoryItemId() != null ? item.getInventoryItemId() : 0),
                    String.valueOf(item.getQuantityDelta() != null ? item.getQuantityDelta() : 0),
                    safe(item.getReason()),
                    item.getAdjustedAt() != null ? item.getAdjustedAt().toString() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf("Stock Adjustments", "INV-ADJ",
                "Stock adjustment export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importStockAdjustmentFromCsv(InputStream csvInputStream) throws IOException {
       
        List<String> errors = new ArrayList<>();
       
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            List<CSVRecord> records = csvParser.getRecords();
            total = records.size();

            for (CSVRecord record : records) {
               
                try {
                    CreateStockAdjustmentRequest request = new CreateStockAdjustmentRequest();
                    request.setInventoryItemId(record.isMapped("INVENTORY_ITEM_ID") && !record.get("INVENTORY_ITEM_ID").isBlank() ? Long.parseLong(record.get("INVENTORY_ITEM_ID").trim()) : null);
                    request.setQuantityDelta(record.isMapped("QUANTITY_DELTA") && !record.get("QUANTITY_DELTA").isBlank() ? new BigDecimal(record.get("QUANTITY_DELTA").trim()) : null);
                    request.setReason(record.isMapped("REASON") ? record.get("REASON") : null);

                    StockAdjustmentResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
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
        String message = isSuccess ? "Import completed successfully. " + success + " out of " + total + " stock adjustments imported." : "Import failed. No stock adjustments were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private StockAdjustmentResponse buildResponse(int statusCode, boolean isSuccess, String message, StockAdjustmentDto dto,
                                                  List<StockAdjustmentDto> dtoList, List<String> errorMessages) {
        StockAdjustmentResponse response = new StockAdjustmentResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setStockAdjustmentDto(dto);
        response.setStockAdjustmentDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private StockAdjustmentResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                            StockAdjustmentDto dto, List<StockAdjustmentDto> dtoList,
                                                            List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }

    private void sendStockAdjustmentCreatedInternal(StockAdjustment stockAdjustment) {
        try {
            Map<String, Object> internalData = Map.of(
                    "stockAdjustmentId", stockAdjustment.getId(),
                    "inventoryItemId", stockAdjustment.getInventoryItem() != null ? stockAdjustment.getInventoryItem().getId() : null,
                    "quantityDelta", stockAdjustment.getQuantityDelta(),
                    "reason", stockAdjustment.getReason(),
                    "adjustedByUserId", stockAdjustment.getAdjustedByUserId()
            );

            NotificationRequest.Recipient internalRecipient = new NotificationRequest.Recipient(
                    null, null, null, null
            );

            NotificationRequest notification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "STOCK_ADJUSTMENT_CREATED_INTERNAL_EMAIL",
                    internalRecipient,
                    internalData,
                    null
            );

            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notification);
            log.info("Sent stock adjustment created internal notification for adjustment {}", stockAdjustment.getId());
        } catch (Exception e) {
            log.error("Failed to send stock adjustment created internal notification for adjustment {}. Error: {}",
                    stockAdjustment.getId(), e.getMessage());
        }
    }

    /**
     * Update inventory using Weighted Average Cost (WAC) method
     * This properly handles opening stock and maintains accurate cost tracking
     */
    private void updateInventoryWithWAC(InventoryItem inventoryItem, BigDecimal quantityDelta,
                                        BigDecimal unitCost) {
        BigDecimal currentStock = inventoryItem.getCurrentStock();
        BigDecimal currentTotalCost = inventoryItem.getTotalCost();

        if (quantityDelta.compareTo(BigDecimal.ZERO) > 0) {
            // STOCK IN
            BigDecimal additionalCost = quantityDelta.multiply(unitCost);
            BigDecimal newStock = currentStock.add(quantityDelta);
            BigDecimal newTotalCost = currentTotalCost.add(additionalCost);

            inventoryItem.setCurrentStock(newStock);
            inventoryItem.setTotalCost(newTotalCost);

            if (newStock.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newAverageCost = newTotalCost.divide(newStock, 4, RoundingMode.HALF_UP);
                inventoryItem.setAverageCost(newAverageCost);
            }
            inventoryItem.setLastPurchaseCost(unitCost);

        } else if (quantityDelta.compareTo(BigDecimal.ZERO) < 0) {
            // STOCK OUT
            BigDecimal quantityOut = quantityDelta.abs();
            BigDecimal costToRemove = quantityOut.multiply(inventoryItem.getAverageCost());
            BigDecimal newStock = currentStock.subtract(quantityOut);
            BigDecimal newTotalCost = currentTotalCost.subtract(costToRemove);

            if (newTotalCost.compareTo(BigDecimal.ZERO) < 0) {
                newTotalCost = BigDecimal.ZERO;
            }

            inventoryItem.setCurrentStock(newStock);
            inventoryItem.setTotalCost(newTotalCost);

            if (newStock.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newAverageCost = newTotalCost.divide(newStock, 4, RoundingMode.HALF_UP);
                inventoryItem.setAverageCost(newAverageCost);
            } else {
                inventoryItem.setTotalCost(BigDecimal.ZERO);
            }
        }

        if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0) {
            inventoryItem.setUnitCost(unitCost);
        }
    }

}
