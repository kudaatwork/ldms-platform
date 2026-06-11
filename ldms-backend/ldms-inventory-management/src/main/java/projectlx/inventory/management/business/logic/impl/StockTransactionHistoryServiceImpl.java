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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.StockTransactionHistoryServiceAuditable;
import projectlx.inventory.management.business.logic.api.StockTransactionHistoryService;
import projectlx.inventory.management.business.validator.api.StockTransactionHistoryServiceValidator;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.model.TransactionType;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.StockTransactionHistorySpecification;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.StockTransactionHistoryDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.requests.EditStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.requests.StockTransactionHistoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.StockTransactionHistoryResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class StockTransactionHistoryServiceImpl implements StockTransactionHistoryService {

    private final StockTransactionHistoryRepository repository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final StockTransactionHistoryServiceAuditable auditable;
    private final StockTransactionHistoryServiceValidator validator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    private static final String[] HEADERS = {"ID", "INVENTORY_ITEM_ID", "WAREHOUSE_LOCATION_ID", "TRANSACTION_TYPE",
            "QUANTITY_CHANGE", "TIMESTAMP", "REASON", "PERFORMED_BY_USER_ID", "REFERENCE_DOCUMENT_ID", "REFERENCE_DOCUMENT_TYPE"};
    private static final String[] CSV_HEADERS = {"INVENTORY_ITEM_ID", "WAREHOUSE_LOCATION_ID", "TRANSACTION_TYPE",
            "QUANTITY_CHANGE", "REASON", "TIMESTAMP", "PERFORMED_BY_USER_ID", "REFERENCE_DOCUMENT_ID", "REFERENCE_DOCUMENT_TYPE"};


    @Override
    public StockTransactionHistoryResponse create(CreateStockTransactionHistoryRequest request, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isCreateStockTransactionHistoryRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<InventoryItem> itemOpt = inventoryItemRepository.findByIdAndEntityStatusNot(request.getInventoryItemId(), EntityStatus.DELETED);
        if (itemOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Optional<WarehouseLocation> locationOpt = warehouseLocationRepository.findById(request.getWarehouseLocationId());
        if (locationOpt.isEmpty() || locationOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        StockTransactionHistory transactionToBeSaved = modelMapper.map(request, StockTransactionHistory.class);
        transactionToBeSaved.setInventoryItem(itemOpt.get());
        transactionToBeSaved.setWarehouseLocation(locationOpt.get());

        StockTransactionHistory saved = auditable.create(transactionToBeSaved, locale, username);
        StockTransactionHistoryDto dto = modelMapper.map(saved, StockTransactionHistoryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public StockTransactionHistoryResponse findById(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<StockTransactionHistory> transactionOpt = repository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (transactionOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        StockTransactionHistoryDto dto = modelMapper.map(transactionOpt.get(), StockTransactionHistoryDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public StockTransactionHistoryResponse findAllAsList(Locale locale, String username) {
        String message;
        List<StockTransactionHistory> list = repository.findByEntityStatusNot(EntityStatus.DELETED);
        if (list.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<StockTransactionHistoryDto> dtoList = list.stream().map(t -> modelMapper.map(t, StockTransactionHistoryDto.class)).collect(Collectors.toList());
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    public StockTransactionHistoryResponse update(EditStockTransactionHistoryRequest request, String username, Locale locale) {
        String message;
        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_STOCK_TRANSACTION_HISTORY_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<StockTransactionHistory> existingOpt = repository.findById(request.getStockTransactionHistoryId());
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        StockTransactionHistory toEdit = existingOpt.get();
        modelMapper.map(request, toEdit);

        StockTransactionHistory saved = auditable.update(toEdit, locale, username);
        StockTransactionHistoryDto dto = modelMapper.map(saved, StockTransactionHistoryDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public StockTransactionHistoryResponse delete(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<StockTransactionHistory> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        StockTransactionHistory toDelete = existingOpt.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);
        StockTransactionHistory saved = auditable.delete(toDelete, locale);

        StockTransactionHistoryDto dto = modelMapper.map(saved, StockTransactionHistoryDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_STOCK_TRANSACTION_HISTORY_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public StockTransactionHistoryResponse findByMultipleFilters(StockTransactionHistoryMultipleFiltersRequest request, String username, Locale locale) {
        // Implementation for filtering logic will be added here
        return null;
    }

    @Override
    public byte[] exportToCsv(List<StockTransactionHistoryDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (StockTransactionHistoryDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(item.getInventoryItemId()).append(",")
                    .append(item.getWarehouseLocationId()).append(",")
                    .append(item.getTransactionType()).append(",")
                    .append(item.getQuantityChange()).append(",")
                    .append(item.getTimestamp()).append(",")
                    .append(item.getReason()).append(",")
                    .append(item.getPerformedByUserId()).append(",")
                    .append(item.getReferenceDocumentId()).append(",")
                    .append(item.getReferenceDocumentType()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<StockTransactionHistoryDto> items) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock Transaction History");
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
        int rowIdx = 1;
        for (StockTransactionHistoryDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(item.getInventoryItemId());
            row.createCell(2).setCellValue(item.getWarehouseLocationId());
            row.createCell(3).setCellValue(item.getTransactionType().name());
            row.createCell(4).setCellValue(item.getQuantityChange().doubleValue());
            row.createCell(5).setCellValue(item.getTimestamp().toString());
            row.createCell(6).setCellValue(item.getReason());
            row.createCell(7).setCellValue(item.getPerformedByUserId());
            row.createCell(8).setCellValue(item.getReferenceDocumentId());
            row.createCell(9).setCellValue(item.getReferenceDocumentType() != null ? item.getReferenceDocumentType().getDisplayName() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<StockTransactionHistoryDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (StockTransactionHistoryDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId()),
                    String.valueOf(item.getInventoryItemId()),
                    String.valueOf(item.getWarehouseLocationId()),
                    item.getTransactionType().name(),
                    String.valueOf(item.getQuantityChange()),
                    item.getTimestamp().toString(),
                    item.getReason(),
                    String.valueOf(item.getPerformedByUserId()),
                    String.valueOf(item.getReferenceDocumentId()),
                    item.getReferenceDocumentType() != null ? item.getReferenceDocumentType().getDisplayName() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf("Stock Transaction History", "INV-TXH",
                "Stock transaction history export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importStockTransactionHistoryFromCsv(InputStream csvInputStream) throws IOException {
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
                    CreateStockTransactionHistoryRequest request = new CreateStockTransactionHistoryRequest();
                    request.setInventoryItemId(record.isMapped("INVENTORY_ITEM_ID") && !record.get("INVENTORY_ITEM_ID").isBlank() ? Long.parseLong(record.get("INVENTORY_ITEM_ID").trim()) : null);
                    request.setWarehouseLocationId(record.isMapped("WAREHOUSE_LOCATION_ID") && !record.get("WAREHOUSE_LOCATION_ID").isBlank() ? Long.parseLong(record.get("WAREHOUSE_LOCATION_ID").trim()) : null);
                    request.setTransactionType(record.isMapped("TRANSACTION_TYPE") && !record.get("TRANSACTION_TYPE").isBlank() ? TransactionType.valueOf(record.get("TRANSACTION_TYPE").trim().toUpperCase()) : null);
                    request.setQuantityChange(record.isMapped("QUANTITY_CHANGE") && !record.get("QUANTITY_CHANGE").isBlank() ? new BigDecimal(record.get("QUANTITY_CHANGE").trim()) : null);
                    request.setTimestamp(record.isMapped("TIMESTAMP") && !record.get("TIMESTAMP").isBlank() ? LocalDateTime.parse(record.get("TIMESTAMP").trim()) : null);
                    request.setReason(record.isMapped("REASON") ? record.get("REASON") : null);
                    request.setPerformedByUserId(record.isMapped("PERFORMED_BY_USER_ID") && !record.get("PERFORMED_BY_USER_ID").isBlank() ? Long.parseLong(record.get("PERFORMED_BY_USER_ID").trim()) : null);
                    request.setReferenceDocumentId(record.isMapped("REFERENCE_DOCUMENT_ID") && !record.get("REFERENCE_DOCUMENT_ID").isBlank() ? Long.parseLong(record.get("REFERENCE_DOCUMENT_ID").trim()) : null);
                    request.setReferenceDocumentType(record.isMapped("REFERENCE_DOCUMENT_TYPE") && !record.get("REFERENCE_DOCUMENT_TYPE").isBlank() ? 
                        ReferenceDocumentType.fromDisplayName(record.get("REFERENCE_DOCUMENT_TYPE").trim()) : null);

                    StockTransactionHistoryResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " stock transaction histories imported."
                : "Import failed. No stock transaction histories were imported.";
        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private StockTransactionHistoryResponse buildResponse(int statusCode, boolean isSuccess, String message, StockTransactionHistoryDto dto, List<StockTransactionHistoryDto> dtoList, List<String> errorMessages) {
        StockTransactionHistoryResponse response = new StockTransactionHistoryResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setStockTransactionHistoryDto(dto);
        response.setStockTransactionHistoryDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private StockTransactionHistoryResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message, StockTransactionHistoryDto dto, List<StockTransactionHistoryDto> dtoList, List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }
}
