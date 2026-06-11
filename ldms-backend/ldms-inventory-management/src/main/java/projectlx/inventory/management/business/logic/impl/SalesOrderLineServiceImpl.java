package projectlx.inventory.management.business.logic.impl;

import com.lowagie.text.DocumentException;
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
import projectlx.inventory.management.business.auditable.api.SalesOrderLineServiceAuditable;
import projectlx.inventory.management.business.logic.api.SalesOrderLineService;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.validator.api.SalesOrderLineServiceValidator;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.SalesOrderLineRepository;
import projectlx.inventory.management.repository.SalesOrderRepository;
import projectlx.inventory.management.repository.specification.SalesOrderLineSpecification;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.SalesOrderLineDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.SalesOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesOrderLineResponse;
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
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class SalesOrderLineServiceImpl implements SalesOrderLineService {

    private final SalesOrderLineRepository salesOrderLineRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final SalesOrderLineServiceValidator validator;
    private final SalesOrderLineServiceAuditable auditable;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    private static final String[] HEADERS = {"ID", "SALES_ORDER_ID", "PRODUCT_ID", "QUANTITY", "UNIT_PRICE",
            "TOTAL_PRICE", "FULFILLED_QUANTITY", "UNIT_OF_MEASURE"};
    private static final String[] CSV_HEADERS = {"SALES_ORDER_ID", "PRODUCT_ID", "QUANTITY", "UNIT_PRICE",
            "UNIT_OF_MEASURE", "FULFILLED_QUANTITY"};

    @Override
    @Transactional
    public SalesOrderLineResponse create(CreateSalesOrderLineRequest request, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isCreateSalesOrderLineRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_SALES_ORDER_LINE_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<SalesOrder> salesOrderOpt = salesOrderRepository.findById(request.getSalesOrderId());
        if (salesOrderOpt.isEmpty() || salesOrderOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrder salesOrder = salesOrderOpt.get();
        // Updated status check to use correct enum values
        if (salesOrder.getStatus() != SalesOrderStatus.PENDING && salesOrder.getStatus() != SalesOrderStatus.CONFIRMED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(request.getProductId(), EntityStatus.DELETED);
        if (productOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesOrderLine salesOrderLineToBeSaved = modelMapper.map(request, SalesOrderLine.class);
        salesOrderLineToBeSaved.setSalesOrder(salesOrder);
        salesOrderLineToBeSaved.setProduct(productOpt.get());
        salesOrderLineToBeSaved.setCreatedByUserId(request.getCreatedByUserId());

        // Calculate total price
        BigDecimal totalPrice = request.getQuantity().multiply(request.getUnitPrice());
        salesOrderLineToBeSaved.setTotalPrice(totalPrice);

        // Initialize fulfilled quantity to zero
        salesOrderLineToBeSaved.setFulfilledQuantity(BigDecimal.ZERO);

        SalesOrderLine saved = auditable.create(salesOrderLineToBeSaved, locale, username);
        SalesOrderLineDto dto = modelMapper.map(saved, SalesOrderLineDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public SalesOrderLineResponse findById(Long id, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<SalesOrderLine> salesOrderLineOpt = salesOrderLineRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (salesOrderLineOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrderLineDto salesOrderLineDto = modelMapper.map(salesOrderLineOpt.get(), SalesOrderLineDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        return buildResponse(200, true, message, salesOrderLineDto, null, null);
    }

    @Override
    public Optional<SalesOrderLine> findSalesOrderLineById(Long id) {
        return salesOrderLineRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
    }

    @Override
    public SalesOrderLineResponse findAllAsList(Locale locale, String username) {

        String message;

        List<SalesOrderLine> list = salesOrderLineRepository.findByEntityStatusNot(EntityStatus.DELETED);
        if (list.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<SalesOrderLineDto> dtoList = list.stream()
                .map(line -> modelMapper.map(line, SalesOrderLineDto.class))
                .collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    @Transactional
    public SalesOrderLineResponse update(EditSalesOrderLineRequest request, String username, Locale locale) {

        String message;

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_SALES_ORDER_LINE_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<SalesOrderLine> existingOpt = salesOrderLineRepository.findById(request.getSalesOrderLineId());
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrderLine toEdit = existingOpt.get();
        // Updated status check to use correct enum values
        if (toEdit.getSalesOrder().getStatus() != SalesOrderStatus.PENDING &&
                toEdit.getSalesOrder().getStatus() != SalesOrderStatus.CONFIRMED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // Update fields
        if (request.getQuantity() != null) {
            toEdit.setQuantity(request.getQuantity());
            // Recalculate total price
            toEdit.setTotalPrice(request.getQuantity().multiply(toEdit.getUnitPrice()));
        }

        if (request.getUnitPrice() != null) {
            toEdit.setUnitPrice(request.getUnitPrice());
            // Recalculate total price
            toEdit.setTotalPrice(toEdit.getQuantity().multiply(request.getUnitPrice()));
        }

        if (request.getUnitOfMeasure() != null) {
            toEdit.setUnitOfMeasure(request.getUnitOfMeasure());
        }

        toEdit.setUpdatedByUserId(request.getUpdatedByUserId());

        SalesOrderLine saved = auditable.update(toEdit, locale, username);
        SalesOrderLineDto salesOrderLineDto = modelMapper.map(saved, SalesOrderLineDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        return buildResponse(200, true, message, salesOrderLineDto, null, null);
    }

    @Override
    @Transactional
    public SalesOrderLine updateFulfilledQuantity(Long salesOrderLineId, BigDecimal quantityFulfilled,
                                                  Long fulfilledByUserId, Locale locale, String username) {

        Optional<SalesOrderLine> existingOpt = salesOrderLineRepository.findById(salesOrderLineId);
        if (existingOpt.isEmpty()) {
            log.error("Failed to update fulfilled quantity: SalesOrderLine with ID {} not found.", salesOrderLineId);
            throw new IllegalArgumentException("Sales order line not found.");
        }

        SalesOrderLine line = existingOpt.get();

        // Ensure the fulfilled quantity does not exceed the ordered quantity
        BigDecimal newFulfilledQuantity = line.getFulfilledQuantity() != null ?
                line.getFulfilledQuantity().add(quantityFulfilled) : quantityFulfilled;

        if (newFulfilledQuantity.compareTo(line.getQuantity()) > 0) {
            log.error("Fulfilled quantity exceeds ordered quantity for line ID {}. Ordered: {}, Fulfilled: {}, New Fulfill: {}",
                    salesOrderLineId, line.getQuantity(), line.getFulfilledQuantity(), quantityFulfilled);
            throw new IllegalArgumentException("Fulfilled quantity cannot exceed ordered quantity.");
        }

        line.setFulfilledQuantity(newFulfilledQuantity);
        line.setUpdatedByUserId(fulfilledByUserId);

        return auditable.update(line, locale, username);
    }

    @Override
    @Transactional
    public SalesOrderLineResponse delete(Long id, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<SalesOrderLine> existingOpt = salesOrderLineRepository.findById(id);
        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrderLine toDelete = existingOpt.get();
        // Updated status check to use correct enum values
        if (toDelete.getSalesOrder().getStatus() != SalesOrderStatus.PENDING &&
                toDelete.getSalesOrder().getStatus() != SalesOrderStatus.CONFIRMED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        toDelete.setEntityStatus(EntityStatus.DELETED);
        SalesOrderLine saved = auditable.delete(toDelete, locale);

        SalesOrderLineDto dto = modelMapper.map(saved, SalesOrderLineDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_DELETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public SalesOrderLineResponse findByMultipleFilters(SalesOrderLineMultipleFiltersRequest request,
                                                        String username, Locale locale) {

        String message;

        ValidatorDto validatorDto = validator.isRequestValidToRetrieveSalesOrderLineByMultipleFilters(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Specification<SalesOrderLine> spec = SalesOrderLineSpecification.deleted();

        if (request.getSalesOrderId() != null) {
            spec = spec.and(SalesOrderLineSpecification.salesOrderIdEquals(request.getSalesOrderId()));
        }

        if (request.getProductId() != null) {
            spec = spec.and(SalesOrderLineSpecification.productIdEquals(request.getProductId()));
        }

        if (request.getUnitOfMeasure() != null) {
            spec = spec.and(SalesOrderLineSpecification.unitOfMeasureEquals(request.getUnitOfMeasure()));
        }

        if (request.getEntityStatus() != null) {
            spec = spec.and(SalesOrderLineSpecification.entityStatusEquals(request.getEntityStatus()));
        }

        long totalCount = salesOrderLineRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage && totalCount > 0) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<SalesOrderLine> result = salesOrderLineRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<SalesOrderLineDto> dtoPage = result.map(line -> modelMapper.map(line, SalesOrderLineDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        SalesOrderLineResponse response = buildResponse(200, true, message, null, null, null);
        response.setSalesOrderLineDtoPage(dtoPage);
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<SalesOrderLineDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (SalesOrderLineDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(item.getSalesOrderId()).append(",")
                    .append(item.getProductId()).append(",")
                    .append(item.getQuantity()).append(",")
                    .append(item.getUnitPrice()).append(",")
                    .append(item.getTotalPrice()).append(",")
                    .append(item.getFulfilledQuantity() != null ? item.getFulfilledQuantity() : "0").append(",")
                    .append(item.getUnitOfMeasure()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<SalesOrderLineDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales Order Lines");
        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (SalesOrderLineDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(item.getSalesOrderId());
            row.createCell(2).setCellValue(item.getProductId());
            row.createCell(3).setCellValue(item.getQuantity().doubleValue());
            row.createCell(4).setCellValue(item.getUnitPrice().doubleValue());
            row.createCell(5).setCellValue(item.getTotalPrice().doubleValue());
            row.createCell(6).setCellValue(item.getFulfilledQuantity() != null ?
                    item.getFulfilledQuantity().doubleValue() : 0);
            row.createCell(7).setCellValue(item.getUnitOfMeasure().name());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<SalesOrderLineDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (SalesOrderLineDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId()),
                    String.valueOf(item.getSalesOrderId()),
                    String.valueOf(item.getProductId()),
                    String.valueOf(item.getQuantity()),
                    String.valueOf(item.getUnitPrice()),
                    String.valueOf(item.getTotalPrice()),
                    String.valueOf(item.getFulfilledQuantity() != null ? item.getFulfilledQuantity() : BigDecimal.ZERO),
                    item.getUnitOfMeasure() != null ? item.getUnitOfMeasure().name() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf(
                "Sales Order Lines", "INV-SOL", "Sales order line export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importSalesOrderLineFromCsv(InputStream csvInputStream) throws IOException {

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
                    CreateSalesOrderLineRequest request = new CreateSalesOrderLineRequest();
                    request.setSalesOrderId(record.isMapped("SALES_ORDER_ID") && !record.get("SALES_ORDER_ID").isBlank() ?
                            Long.parseLong(record.get("SALES_ORDER_ID").trim()) : null);
                    request.setProductId(record.isMapped("PRODUCT_ID") && !record.get("PRODUCT_ID").isBlank() ?
                            Long.parseLong(record.get("PRODUCT_ID").trim()) : null);
                    request.setQuantity(record.isMapped("QUANTITY") && !record.get("QUANTITY").isBlank() ?
                            new BigDecimal(record.get("QUANTITY").trim()) : null);
                    request.setUnitPrice(record.isMapped("UNIT_PRICE") && !record.get("UNIT_PRICE").isBlank() ?
                            new BigDecimal(record.get("UNIT_PRICE").trim()) : null);
                    request.setUnitOfMeasure(record.isMapped("UNIT_OF_MEASURE") && !record.get("UNIT_OF_MEASURE").isBlank() ?
                            UnitOfMeasure.valueOf(record.get("UNIT_OF_MEASURE").trim().toUpperCase()) : null);

                    SalesOrderLineResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
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
                ? "Import completed successfully. " + success + " out of " + total + " sales order lines imported."
                : "Import failed. No sales order lines were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private SalesOrderLineResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                                 SalesOrderLineDto dto, List<SalesOrderLineDto> dtoList,
                                                 List<String> errorMessages) {
        SalesOrderLineResponse response = new SalesOrderLineResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setSalesOrderLineDto(dto);
        response.setSalesOrderLineDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private SalesOrderLineResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                           SalesOrderLineDto dto, List<SalesOrderLineDto> dtoList,
                                                           List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }
}