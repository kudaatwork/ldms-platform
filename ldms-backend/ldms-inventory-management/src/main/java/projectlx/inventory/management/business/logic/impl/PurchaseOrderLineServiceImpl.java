package projectlx.inventory.management.business.logic.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import projectlx.inventory.management.business.auditable.api.PurchaseOrderLineServiceAuditable;
import projectlx.inventory.management.business.logic.api.PurchaseOrderLineService;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.validator.api.PurchaseOrderLineServiceValidator;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.PurchaseOrderLine;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.PurchaseOrderLineRepository;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.repository.specification.PurchaseOrderLineSpecification;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.PurchaseOrderLineDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderLineResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.csv.CSVParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderLineServiceImpl implements PurchaseOrderLineService {

    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderLineServiceValidator validator;
    private final PurchaseOrderLineServiceAuditable auditable;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    private static final String[] HEADERS = {"ID", "PURCHASE_ORDER_ID", "PRODUCT_ID", "QUANTITY", "UNIT_PRICE",
            "TOTAL_PRICE", "RECEIVED_QUANTITY", "UNIT_OF_MEASURE"};
    private static final String[] CSV_HEADERS = {"PURCHASE_ORDER_ID", "PRODUCT_ID", "QUANTITY", "UNIT_PRICE",
            "UNIT_OF_MEASURE", "RECEIVED_QUANTITY"};

    @Override
    public PurchaseOrderLineResponse create(CreatePurchaseOrderLineRequest request, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = validator.isCreatePurchaseOrderLineRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrder> purchaseOrderOpt = purchaseOrderRepository.findById(request.getPurchaseOrderId());
        if (purchaseOrderOpt.isEmpty() || purchaseOrderOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrder purchaseOrder = purchaseOrderOpt.get();
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT && purchaseOrder.getStatus() != PurchaseOrderStatus.SUBMITTED) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(request.getProductId(), EntityStatus.DELETED);
        if (productOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        PurchaseOrderLine purchaseOrderLineToBeSaved = modelMapper.map(request, PurchaseOrderLine.class);
        purchaseOrderLineToBeSaved.setPurchaseOrder(purchaseOrder);
        purchaseOrderLineToBeSaved.setProduct(productOpt.get());
        purchaseOrderLineToBeSaved.setCreatedByUserId(purchaseOrder.getCreatedByUserId());
        Integer maxLineNumber = purchaseOrderLineRepository.findMaxLineNumberByPurchaseOrderId(purchaseOrder.getId());
        purchaseOrderLineToBeSaved.setLineNumber((maxLineNumber != null ? maxLineNumber : 0) + 1);

        PurchaseOrderLine saved = auditable.create(purchaseOrderLineToBeSaved, locale, username);
        PurchaseOrderLineDto dto = modelMapper.map(saved, PurchaseOrderLineDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public PurchaseOrderLineResponse findById(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrderLine> purchaseOrderLineOpt = purchaseOrderLineRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (purchaseOrderLineOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrderLineDto purchaseOrderLineDto = modelMapper.map(purchaseOrderLineOpt.get(), PurchaseOrderLineDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, purchaseOrderLineDto, null, null);
    }

    @Override
    public Optional<PurchaseOrderLine> findPurchaseOrderLineById(Long id) {
        return purchaseOrderLineRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
    }

    @Override
    public PurchaseOrderLineResponse findAllAsList(Locale locale, String username) {
        String message;
        List<PurchaseOrderLine> list = purchaseOrderLineRepository.findByEntityStatusNot(EntityStatus.DELETED);
        if (list.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<PurchaseOrderLineDto> dtoList = list.stream().map(pol -> modelMapper.map(pol, PurchaseOrderLineDto.class)).collect(Collectors.toList());
        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    public PurchaseOrderLineResponse update(EditPurchaseOrderLineRequest request, String username, Locale locale) {
        String message;
        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PURCHASE_ORDER_LINE_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrderLine> existingOpt = purchaseOrderLineRepository.findById(request.getPurchaseOrderLineId());
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrderLine toEdit = existingOpt.get();
        if (toEdit.getPurchaseOrder().getStatus() != PurchaseOrderStatus.DRAFT && toEdit.getPurchaseOrder().getStatus() != PurchaseOrderStatus.SUBMITTED) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        modelMapper.map(request, toEdit);
        toEdit.setUpdatedByUserId(request.getUpdatedByUserId());

        PurchaseOrderLine saved = auditable.update(toEdit, locale, username);
        PurchaseOrderLineDto purchaseOrderLineDto = modelMapper.map(saved, PurchaseOrderLineDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, purchaseOrderLineDto, null, null);
    }

    @Override
    @Transactional
    public PurchaseOrderLine updateReceivedQuantity(Long purchaseOrderLineId, BigDecimal quantityReceived, Long receivedByUserId, Locale locale, String username) {
        // Re-load with a write lock to prevent races
        Optional<PurchaseOrderLine> existingOpt = purchaseOrderLineRepository.findByIdForUpdate(purchaseOrderLineId);
        if (existingOpt.isEmpty()) {
            log.error("Failed to update received quantity: PurchaseOrderLine with ID {} not found.", purchaseOrderLineId);
            throw new IllegalArgumentException("Purchase order line not found.");
        }

        PurchaseOrderLine line = existingOpt.get();

        BigDecimal receivedToDate = line.getReceivedQuantity() == null ? BigDecimal.ZERO : line.getReceivedQuantity();
        BigDecimal orderedQty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();

        // Ensure the received quantity does not exceed the remaining quantity
        BigDecimal newReceivedQuantity = receivedToDate.add(quantityReceived);
        if (newReceivedQuantity.compareTo(orderedQty) > 0) {
            BigDecimal remaining = orderedQty.subtract(receivedToDate).max(BigDecimal.ZERO);
            log.error("Over-receipt not allowed for line ID {}. Ordered: {}, Received to date: {}, Attempted: {}, Remaining: {}",
                    purchaseOrderLineId, orderedQty, receivedToDate, quantityReceived, remaining);
            throw new IllegalArgumentException("Received quantity cannot exceed ordered quantity.");
        }

        line.setReceivedQuantity(newReceivedQuantity);
        line.setUpdatedByUserId(receivedByUserId);

        return auditable.update(line, locale, username);
    }

    @Override
    public PurchaseOrderLineResponse delete(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrderLine> existingOpt = purchaseOrderLineRepository.findById(id);
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrderLine toDelete = existingOpt.get();
        if (toDelete.getPurchaseOrder().getStatus() != PurchaseOrderStatus.DRAFT && toDelete.getPurchaseOrder().getStatus() != PurchaseOrderStatus.SUBMITTED) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        toDelete.setEntityStatus(EntityStatus.DELETED);
        PurchaseOrderLine saved = auditable.delete(toDelete, locale);
        PurchaseOrderLineDto dto = modelMapper.map(saved, PurchaseOrderLineDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public PurchaseOrderLineResponse findByMultipleFilters(PurchaseOrderLineMultipleFiltersRequest request, String username, Locale locale) {
        String message;
        ValidatorDto validatorDto = validator.isRequestValidToRetrievePurchaseOrderLineByMultipleFilters(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }
        Specification<PurchaseOrderLine> spec = PurchaseOrderLineSpecification.deleted();
        if (request.getUnitOfMeasure() != null) {
            spec = spec.and(PurchaseOrderLineSpecification.unitOfMeasureEquals(request.getUnitOfMeasure()));
        }
        if (request.getEntityStatus() != null) {
            spec = spec.and(PurchaseOrderLineSpecification.entityStatusEquals(request.getEntityStatus()));
        }
        long totalCount = purchaseOrderLineRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());
        if (request.getPage() >= maxPage && totalCount > 0) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<PurchaseOrderLine> result = purchaseOrderLineRepository.findAll(spec, pageable);
        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<PurchaseOrderLineDto> dtoPage = result.map(pol -> modelMapper.map(pol, PurchaseOrderLineDto.class));
        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        PurchaseOrderLineResponse response = buildResponse(200, true, message, null, null, null);
        response.setPurchaseOrderLineDtoPage(dtoPage);
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<PurchaseOrderLineDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (PurchaseOrderLineDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(item.getPurchaseOrderId()).append(",")
                    .append(item.getProductId()).append(",")
                    .append(item.getQuantity()).append(",")
                    .append(item.getUnitPrice()).append(",")
                    .append(item.getTotalPrice()).append(",")
                    .append(item.getReceivedQuantity()).append(",")
                    .append(item.getUnitOfMeasure()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<PurchaseOrderLineDto> items) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchase Order Lines");
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
        int rowIdx = 1;
        for (PurchaseOrderLineDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(item.getPurchaseOrderId());
            row.createCell(2).setCellValue(item.getProductId());
            row.createCell(3).setCellValue(item.getQuantity().doubleValue());
            row.createCell(4).setCellValue(item.getUnitPrice().doubleValue());
            row.createCell(5).setCellValue(item.getTotalPrice().doubleValue());
            row.createCell(6).setCellValue(item.getReceivedQuantity().doubleValue());
            row.createCell(7).setCellValue(item.getUnitOfMeasure().name());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<PurchaseOrderLineDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (PurchaseOrderLineDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId()),
                    String.valueOf(item.getPurchaseOrderId()),
                    String.valueOf(item.getProductId()),
                    String.valueOf(item.getQuantity()),
                    String.valueOf(item.getUnitPrice()),
                    String.valueOf(item.getTotalPrice()),
                    String.valueOf(item.getReceivedQuantity()),
                    item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf(
                "Purchase Order Lines", "INV-POL", "Purchase order line export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importPurchaseOrderLineFromCsv(InputStream csvInputStream) throws IOException {
        java.util.List<String> errors = new java.util.ArrayList<>();
        int success = 0, failed = 0, total = 0;
        try (java.io.Reader reader = new java.io.InputStreamReader(csvInputStream, java.nio.charset.StandardCharsets.UTF_8);
             CSVParser csvParser = org.apache.commons.csv.CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {
            java.util.List<CSVRecord> records = csvParser.getRecords();
            total = records.size();
            for (CSVRecord record : records) {
                try {
                    CreatePurchaseOrderLineRequest request = new CreatePurchaseOrderLineRequest();
                    request.setPurchaseOrderId(record.isMapped("PURCHASE_ORDER_ID") && !record.get("PURCHASE_ORDER_ID").isBlank() ? Long.parseLong(record.get("PURCHASE_ORDER_ID").trim()) : null);
                    request.setProductId(record.isMapped("PRODUCT_ID") && !record.get("PRODUCT_ID").isBlank() ? Long.parseLong(record.get("PRODUCT_ID").trim()) : null);
                    request.setQuantity(record.isMapped("QUANTITY") && !record.get("QUANTITY").isBlank() ? new BigDecimal(record.get("QUANTITY").trim()) : null);
                    request.setUnitPrice(record.isMapped("UNIT_PRICE") && !record.get("UNIT_PRICE").isBlank() ? new BigDecimal(record.get("UNIT_PRICE").trim()) : null);
                    request.setUnitOfMeasure(record.isMapped("UNIT_OF_MEASURE") && !record.get("UNIT_OF_MEASURE").isBlank() ? UnitOfMeasure.valueOf(record.get("UNIT_OF_MEASURE").trim().toUpperCase()) : null);
                    request.setReceivedQuantity(record.isMapped("RECEIVED_QUANTITY") && !record.get("RECEIVED_QUANTITY").isBlank() ? new BigDecimal(record.get("RECEIVED_QUANTITY").trim()) : null);
                    PurchaseOrderLineResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " purchase order lines imported."
                : "Import failed. No purchase order lines were imported.";
        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private PurchaseOrderLineResponse buildResponse(int statusCode, boolean isSuccess, String message, PurchaseOrderLineDto dto, List<PurchaseOrderLineDto> dtoList, List<String> errorMessages) {
        PurchaseOrderLineResponse response = new PurchaseOrderLineResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setPurchaseOrderLineDto(dto);
        response.setPurchaseOrderLineDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private PurchaseOrderLineResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message, PurchaseOrderLineDto dto, List<PurchaseOrderLineDto> dtoList, List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }
}
