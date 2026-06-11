package projectlx.inventory.management.business.logic.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import projectlx.inventory.management.business.auditable.api.InventoryItemServiceAuditable;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.auditable.api.StockTransactionHistoryServiceAuditable;
import projectlx.inventory.management.business.logic.api.ConcurrentInventoryHandler;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.validator.api.InventoryItemServiceValidator;
import projectlx.inventory.management.clients.OrganizationServiceClient;
import projectlx.inventory.management.clients.UserManagementServiceClient;
import projectlx.inventory.management.exceptions.InsufficientInventoryException;
import projectlx.inventory.management.exceptions.InventoryNotFoundException;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.model.TransactionType;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.InventoryItemSpecification;
import projectlx.inventory.management.utils.dtos.InventoryItemDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.enums.StockLevelStatus;
import projectlx.inventory.management.utils.requests.CreateInitialStockRequest;
import projectlx.inventory.management.utils.requests.InventoryItemMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryItemRequest;
import projectlx.inventory.management.utils.requests.EditInventoryItemRequest;
import projectlx.inventory.management.utils.requests.CreateOrUpdateStockRequest;
import projectlx.inventory.management.utils.requests.NotificationRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class InventoryItemServiceImpl implements InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final StockTransactionHistoryServiceAuditable stockTransactionHistoryServiceAuditable;
    private final InventoryItemServiceAuditable inventoryItemServiceAuditable;
    private final ConcurrentInventoryHandler concurrentInventoryHandler;
    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final InventoryItemServiceValidator validator;
    private final RabbitTemplate rabbitTemplate;
    private final OrganizationServiceClient organizationServiceClient;
    private final UserManagementServiceClient userManagementServiceClient;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final InventoryItemService self;

    private static final String[] HEADERS = {
            "ID", "PRODUCT_ID", "PRODUCT_CODE", "BARCODE", "PRODUCT_NAME", "WAREHOUSE_LOCATION_ID", "WAREHOUSE_NAME",
            "SUPPLIER_ID", "CURRENT_STOCK", "RESERVED_QUANTITY", "AVAILABLE_QUANTITY", "UNIT_OF_MEASURE",
            "MIN_STOCK_LEVEL", "REORDER_QUANTITY", "STOCK_STATUS", "BATCH_LOT", "SERIAL_NUMBER", "EXPIRES_AT"
    };
    private static final String[] CSV_HEADERS = {
            "PRODUCT_ID", "PRODUCT_CODE", "BARCODE", "WAREHOUSE_LOCATION_ID", "SUPPLIER_ID", "CURRENT_STOCK",
            "MIN_STOCK_LEVEL", "REORDER_QUANTITY", "BATCH_LOT", "SERIAL_NUMBER", "EXPIRES_AT"
    };

    @Override
    public InventoryItemResponse create(CreateInventoryItemRequest request, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isCreateInventoryItemRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_ITEM_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<Product> productRetrieved = productRepository.findByIdAndEntityStatusNot(request.getProductId(),
                EntityStatus.DELETED);

        if (productRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Optional<WarehouseLocation> locationOpt = warehouseLocationRepository.findByIdAndEntityStatusNot(
                request.getWarehouseLocationId(), EntityStatus.DELETED);

        if (locationOpt.isEmpty() || locationOpt.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Optional<InventoryItem> existingItemOpt = inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                request.getProductId(), request.getWarehouseLocationId(), EntityStatus.DELETED);

        if (existingItemOpt.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_ALREADY_EXISTS_FOR_LOCATION.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null, null);
        }

        Long supplierId = resolveSupplierId(request.getSupplierId(), productRetrieved.get(), locationOpt.get());
        if (supplierId == null) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_PRODUCT_SUPPLIER_ID_INVALID.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        InventoryItem itemToBeSaved = modelMapper.map(request, InventoryItem.class);
        itemToBeSaved.setProduct(productRetrieved.get());
        itemToBeSaved.setWarehouseLocation(locationOpt.get());
        itemToBeSaved.setSupplierId(supplierId);
        itemToBeSaved.setCreatedByUserId(request.getCreatedByUserId());
        itemToBeSaved.setCurrentStock(request.getCurrentStock() != null ? request.getCurrentStock() : BigDecimal.ZERO);
        itemToBeSaved.setQuantity(itemToBeSaved.getCurrentStock());
        itemToBeSaved.setMinStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : BigDecimal.ZERO);
        itemToBeSaved.setReorderQuantity(request.getReorderQuantity() != null ? request.getReorderQuantity() : BigDecimal.ZERO);
        itemToBeSaved.setReservedQuantity(BigDecimal.ZERO);
        itemToBeSaved.setTotalCost(BigDecimal.ZERO);
        itemToBeSaved.setAverageCost(BigDecimal.ZERO);
        itemToBeSaved.setBatchLot(request.getBatchLot());
        itemToBeSaved.setSerialNumber(request.getSerialNumber());
        itemToBeSaved.setExpiresAt(request.getExpiresAt());

        InventoryItem saved = inventoryItemServiceAuditable.create(itemToBeSaved, locale, username);
        InventoryItemDto dto = mapToDto(saved);

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildResponse(201, true, message, dto, null, null);
    }

    @Override
    public InventoryItemResponse findById(Long id, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponse(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<InventoryItem> itemOpt = inventoryItemRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (itemOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        InventoryItemDto dto = mapToDto(itemOpt.get());

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public InventoryItemResponse findAllAsList(Locale locale, String username) {

        String message;

        List<InventoryItem> list = inventoryItemRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if (list.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        List<InventoryItemDto> dtoList = list.stream().map(this::mapToDto).collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, null, dtoList, null);
    }

    @Override
    public InventoryItemResponse update(EditInventoryItemRequest request, String username, Locale locale) {

        String message;

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_INVENTORY_ITEM_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponse(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<InventoryItem> existingOpt = inventoryItemRepository.findById(request.getInventoryItemId());

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        InventoryItem toEdit = existingOpt.get();
        modelMapper.map(request, toEdit);
        toEdit.setUpdatedByUserId(request.getUpdatedByUserId());

        InventoryItem saved = inventoryItemServiceAuditable.update(toEdit, locale, username);
        InventoryItemDto dto = mapToDto(saved);

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public InventoryItemResponse delete(Long id, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponse(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<InventoryItem> existingOpt = inventoryItemRepository.findById(id);

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        InventoryItem toDelete = existingOpt.get();
        toDelete.setEntityStatus(EntityStatus.DELETED);
        InventoryItem saved = inventoryItemServiceAuditable.delete(toDelete, locale);

        InventoryItemDto inventoryItemDto = mapToDto(saved);

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildResponse(200, true, message, inventoryItemDto, null, null);
    }

    @Override
    public InventoryItemResponse findByMultipleFilters(InventoryItemMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";
        Specification<InventoryItem> spec = null;
        spec = addToSpec(spec, InventoryItemSpecification::deleted);

        ValidatorDto validatorDto = validator.isRequestValidToRetrieveInventoryItemByMultipleFilters(request, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (request.getProductId() != null) {

            spec = (spec == null)
                    ? InventoryItemSpecification.productIdEquals(request.getProductId())
                    : spec.and(InventoryItemSpecification.productIdEquals(request.getProductId()));
        }

        if (request.getWarehouseLocationId() != null) {

            spec = (spec == null)
                    ? InventoryItemSpecification.warehouseLocationIdEquals(request.getWarehouseLocationId())
                    : spec.and(InventoryItemSpecification.warehouseLocationIdEquals(request.getWarehouseLocationId()));
        }

        if (request.getSupplierId() != null) {

            spec = (spec == null)
                    ? InventoryItemSpecification.supplierIdEquals(request.getSupplierId())
                    : spec.and(InventoryItemSpecification.supplierIdEquals(request.getSupplierId()));
        }

        ValidatorDto batchLotValidatorDto = validator.isStringValid(request.getBatchLot(), locale);

        if (batchLotValidatorDto.getSuccess()) {

            spec = addToSpec(request.getBatchLot(), spec, InventoryItemSpecification::batchLotLike);
        }

        ValidatorDto serialNumberValidatorDto = validator.isStringValid(request.getSerialNumber(), locale);

        if (serialNumberValidatorDto.getSuccess()) {

            spec = addToSpec(request.getSerialNumber(), spec, InventoryItemSpecification::serialNumberLike);
        }

        if (request.getEntityStatus() != null) {

            spec = (spec == null)
                    ? InventoryItemSpecification.entityStatusEquals(request.getEntityStatus())
                    : spec.and(InventoryItemSpecification.entityStatusEquals(request.getEntityStatus()));
        }

        if (request.getStockStatus() != null) {
            spec = (spec == null)
                    ? InventoryItemSpecification.stockStatusEquals(request.getStockStatus())
                    : spec.and(InventoryItemSpecification.stockStatusEquals(request.getStockStatus()));
        }

        if (request.getSearchValue() != null && !request.getSearchValue().trim().isEmpty()) {
            spec = (spec == null)
                    ? InventoryItemSpecification.any(request.getSearchValue())
                    : spec.and(InventoryItemSpecification.any(request.getSearchValue()));
        }

        long totalCount = inventoryItemRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage && totalCount > 0) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Page<InventoryItem> result = inventoryItemRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                    locale);
            InventoryItemResponse response = buildResponse(200, true, message, null, null, null);
            response.setInventoryItemDtoPage(new PageImpl<>(List.of(), pageable, totalCount));
            return response;
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<InventoryItemDto> inventoryItemDtoPage = result.map(this::mapToDto);

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        InventoryItemResponse response = buildResponse(200, true, message, null, null,
                null);
        response.setInventoryItemDtoPage(inventoryItemDtoPage);
        return response;
    }

    @Override
    @Transactional
    public void createOrUpdateStock(CreateOrUpdateStockRequest request, Locale locale, String username) {
        Long productId = request.getProductId();
        Long warehouseLocationId = request.getWarehouseLocationId();
        BigDecimal quantityReceived = request.getQuantityReceived();
        String reason = request.getReason();
        Long userId = request.getUserId();
        Long referenceDocumentId = request.getReferenceDocumentId();
        ReferenceDocumentType referenceDocumentType = request.getReferenceDocumentType();
        BigDecimal unitCost = request.getUnitCost();

        // VALIDATION: Product must exist
        Optional<Product> productOpt = productRepository
                .findByIdAndEntityStatusNot(productId, EntityStatus.DELETED);
        if (productOpt.isEmpty()) {
            log.error("Product not found: productId={}", productId);
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        // VALIDATION: Warehouse must exist
        Optional<WarehouseLocation> locationOpt = warehouseLocationRepository
                .findByIdAndEntityStatusNot(warehouseLocationId, EntityStatus.DELETED);
        if (locationOpt.isEmpty()) {
            log.error("Warehouse location not found: warehouseLocationId={}", warehouseLocationId);
            throw new IllegalArgumentException("Warehouse location not found: " + warehouseLocationId);
        }

        // CRITICAL: InventoryItem MUST exist - DO NOT auto-create
        Optional<InventoryItem> inventoryItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        productId, warehouseLocationId, EntityStatus.DELETED);

        if (inventoryItemOpt.isEmpty()) {
            String errorMsg = String.format(
                    "Inventory not set up for product %d at warehouse %d. " +
                            "Please create inventory item first through inventory setup process.",
                    productId, warehouseLocationId);
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        try {
            InventoryItem inventoryItem = inventoryItemOpt.get();

            // Use concurrent handler for thread-safe locking
            InventoryItem updatedItem = concurrentInventoryHandler.updateInventoryWithLocking(
                    inventoryItem.getId(),
                    quantityReceived,
                    unitCost,
                    quantityReceived.compareTo(BigDecimal.ZERO) >= 0 ? "STOCK_IN" : "STOCK_OUT"
            );

            log.info("Stock transaction successful: product={}, warehouse={}, quantity={}, newStock={}, avgCost={}",
                    productId, warehouseLocationId, quantityReceived,
                    updatedItem.getCurrentStock(), updatedItem.getAverageCost());

            // Create transaction history (audit trail)
            createStockTransactionHistory(updatedItem, quantityReceived, unitCost, userId,
                    referenceDocumentId, referenceDocumentType, reason, locale, username);

        } catch (InsufficientInventoryException e) {
            log.error("Insufficient inventory: product={}, warehouse={}, required={}",
                    productId, warehouseLocationId, quantityReceived.abs());
            throw new IllegalArgumentException("Insufficient inventory: " + e.getMessage(), e);

        } catch (InventoryNotFoundException e) {
            log.error("Inventory disappeared during update: product={}, warehouse={}",
                    productId, warehouseLocationId);
            throw new IllegalArgumentException("Inventory item not found during update", e);

        } catch (Exception e) {
            log.error("Stock transaction failed: product={}, warehouse={}, error={}",
                    productId, warehouseLocationId, e.getMessage(), e);
            throw new RuntimeException("Stock transaction failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<WarehouseLocation> findWarehouseLocationBySupplierId(Long supplierId, Long warehouseLocationId) {
        return warehouseLocationRepository.findByIdAndSupplierIdAndEntityStatusNot(warehouseLocationId, supplierId, EntityStatus.DELETED);
    }

    @Override
    public InventoryItemResponse createStockOut(Long inventoryItemId, BigDecimal quantity, String reason, Long userId,
                                                Long referenceDocumentId, ReferenceDocumentType referenceDocumentType,
                                                Locale locale, String username) {
        String message;

        try {
            // Use concurrent handler for thread-safe stock out operation
            InventoryItem updatedItem = concurrentInventoryHandler.updateInventoryWithLocking(
                    inventoryItemId,
                    quantity.negate(), // The handler expects positive quantity and determines direction by operation type
                    BigDecimal.ZERO, // Unit cost not needed for stock out (uses existing average cost)
                    "STOCK_OUT"
            );

            // Create stock transaction history
            createStockTransactionHistory(updatedItem, quantity.negate(), updatedItem.getAverageCost(),
                    userId, referenceDocumentId, referenceDocumentType, reason, locale, username);

            // Build successful response
            InventoryItemDto dto = mapToDto(updatedItem);
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_UPDATED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            log.info("Successfully processed stock out for inventory item {}: {} units",
                    inventoryItemId, quantity);

            return buildResponse(200, true, message, dto, null, null);

        } catch (InventoryNotFoundException e) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            log.error("Inventory item not found for stock out: {}", inventoryItemId);
            return buildResponse(404, false, message, null, null, null);

        } catch (InsufficientInventoryException e) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_INSUFFICIENT_STOCK.getCode(),
                    new String[]{}, locale);
            log.error("Insufficient stock for item {}: {}", inventoryItemId, e.getMessage());
            return buildResponse(400, false, message, null, null, null);

        } catch (Exception e) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_UPDATE_FAILED.getCode(),
                    new String[]{e.getMessage()}, locale);
            log.error("Unexpected error during stock out for item {}: {}", inventoryItemId, e.getMessage(), e);
            return buildResponse(500, false, message, null, null, null);
        }
    }

    @Override
    public InventoryItemResponse recordPurchaseReturn(Long inventoryItemId, BigDecimal quantityReturned,
                                                      String reason, Long userId, Long referenceDocumentId,
                                                      ReferenceDocumentType referenceDocumentType,
                                                      BigDecimal unitCost, Locale locale, String username) {
        String message;

        Optional<InventoryItem> existingItemOpt = inventoryItemRepository
                .findByIdAndEntityStatusNot(inventoryItemId, EntityStatus.DELETED);

        if (existingItemOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryItem item = existingItemOpt.get();

        // Use concurrent handler for thread-safe update
        try {
            // ✅ FIX: Convert positive quantity to negative for STOCK_OUT operation
            // User input: quantityReturned = 5
            // We need: quantity_change = -5 (to decrease inventory)
            BigDecimal negativeQuantity = quantityReturned.negate();

            log.info("Recording purchase return - Product ID: {}, Quantity Returned: {}, Converted to: {}",
                    item.getProduct().getId(), quantityReturned, negativeQuantity);

            // ✅ FIX: Use STOCK_OUT instead of STOCK_IN (return = inventory going OUT)
            InventoryItem updatedItem = concurrentInventoryHandler.updateInventoryWithLocking(
                    inventoryItemId,
                    negativeQuantity,           // ✅ NEGATIVE value (-5)
                    unitCost,
                    "STOCK_OUT"                 // ✅ STOCK_OUT (not STOCK_IN)
            );

            // Create transaction history
            StockTransactionHistory transaction = new StockTransactionHistory();
            transaction.setInventoryItem(updatedItem);

            // ✅ FIX: Use STOCK_OUT transaction type
            transaction.setTransactionType(TransactionType.STOCK_OUT);  // ✅ Changed from STOCK_IN

            // ✅ FIX: Store negative quantity_change for audit trail
            transaction.setQuantityChange(negativeQuantity);            // ✅ Negative value (-5)

            transaction.setUnitCost(unitCost);
            transaction.setTimestamp(LocalDateTime.now());
            transaction.setWarehouseLocation(updatedItem.getWarehouseLocation());
            transaction.setPerformedByUserId(userId);
            transaction.setReferenceDocumentId(referenceDocumentId);
            transaction.setReferenceDocumentType(referenceDocumentType);
            transaction.setReason(reason != null ? reason : "Purchase return");

            stockTransactionHistoryServiceAuditable.create(transaction, locale, username);

            log.info("Purchase return recorded successfully - Inventory Item ID: {}, New Quantity: {}, New Total Cost: {}",
                    updatedItem.getId(), updatedItem.getQuantity(), updatedItem.getTotalCost());

            InventoryItemDto dto = mapToDto(updatedItem);
            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_RECORDED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            return buildResponse(200, true, message, dto, null, null);

        } catch (InsufficientInventoryException e) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_INSUFFICIENT_STOCK.getCode(),
                    new String[]{}, locale);
            log.error("Insufficient inventory for return: Product ID: {}, Available: {}, Requested Return: {}",
                    item.getProduct().getId(), item.getQuantity(), quantityReturned);
            return buildResponse(400, false, message, null, null, null);

        } catch (Exception e) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_UPDATE_FAILED.getCode(),
                    new String[]{e.getMessage()}, locale);
            log.error("Failed to record purchase return for inventory item {}: {}", inventoryItemId, e.getMessage(), e);
            return buildResponse(500, false, message, null, null, null);
        }
    }

    // New method to be added as requested by the user
    @Override
    public Optional<InventoryItem> findInventoryItemByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        return inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(productId, warehouseId,
                EntityStatus.DELETED);
    }

    private InventoryItemResponse buildResponse(int statusCode, boolean isSuccess, String message, InventoryItemDto dto,
                                                List<InventoryItemDto> dtoList, List<String> errorMessages) {
        InventoryItemResponse response = new InventoryItemResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setInventoryItemDto(dto);
        response.setInventoryItemDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private InventoryItemResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                          InventoryItemDto dto, List<InventoryItemDto> dtoList,
                                                          List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private Specification<InventoryItem> addToSpec(
            Specification<InventoryItem> spec,
            Function<EntityStatus, Specification<InventoryItem>> predicateMethod) {
        return spec == null ? predicateMethod.apply(EntityStatus.DELETED) : spec.and(predicateMethod.apply(EntityStatus.DELETED));
    }

    private Specification<InventoryItem> addToSpec(
            String aString,
            Specification<InventoryItem> spec,
            Function<String, Specification<InventoryItem>> predicateMethod) {
        if (aString == null || aString.trim().isEmpty()) return spec;
        String value = aString.toUpperCase();
        return spec == null ? predicateMethod.apply(value) : spec.and(predicateMethod.apply(value));
    }

    @Override
    public byte[] exportToCsv(List<InventoryItemDto> items) {
        items = InventoryExportSupport.nullSafe(items);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (InventoryItemDto item : items) {
            sb.append(item.getId() != null ? item.getId() : "").append(",")
                    .append(item.getProductId() != null ? item.getProductId() : "").append(",")
                    .append(safe(item.getProductCode())).append(",")
                    .append(safe(item.getProductBarcode())).append(",")
                    .append(safe(item.getProductName())).append(",")
                    .append(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : "").append(",")
                    .append(safe(item.getWarehouseLocationName())).append(",")
                    .append(item.getSupplierId() != null ? item.getSupplierId() : "").append(",")
                    .append(item.getCurrentStock() != null ? item.getCurrentStock() : "").append(",")
                    .append(item.getReservedQuantity() != null ? item.getReservedQuantity() : "").append(",")
                    .append(item.getAvailableQuantity() != null ? item.getAvailableQuantity() : "").append(",")
                    .append(safe(item.getUnitOfMeasure())).append(",")
                    .append(item.getMinStockLevel() != null ? item.getMinStockLevel() : "").append(",")
                    .append(item.getReorderQuantity() != null ? item.getReorderQuantity() : "").append(",")
                    .append(item.getStockStatus() != null ? item.getStockStatus().name() : "").append(",")
                    .append(safe(item.getBatchLot())).append(",")
                    .append(safe(item.getSerialNumber())).append(",")
                    .append(item.getExpiresAt() != null ? item.getExpiresAt().toString() : "").append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<InventoryItemDto> items) throws IOException {
        items = InventoryExportSupport.nullSafe(items);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory Items");
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
        int rowIdx = 1;
        for (InventoryItemDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            row.createCell(col++).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(col++).setCellValue(item.getProductId() != null ? item.getProductId() : 0);
            row.createCell(col++).setCellValue(safe(item.getProductCode()));
            row.createCell(col++).setCellValue(safe(item.getProductBarcode()));
            row.createCell(col++).setCellValue(safe(item.getProductName()));
            row.createCell(col++).setCellValue(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : 0);
            row.createCell(col++).setCellValue(safe(item.getWarehouseLocationName()));
            row.createCell(col++).setCellValue(item.getSupplierId() != null ? item.getSupplierId() : 0);
            row.createCell(col++).setCellValue(item.getCurrentStock() != null ? item.getCurrentStock().doubleValue() : 0);
            row.createCell(col++).setCellValue(item.getReservedQuantity() != null ? item.getReservedQuantity().doubleValue() : 0);
            row.createCell(col++).setCellValue(item.getAvailableQuantity() != null ? item.getAvailableQuantity().doubleValue() : 0);
            row.createCell(col++).setCellValue(safe(item.getUnitOfMeasure()));
            row.createCell(col++).setCellValue(item.getMinStockLevel() != null ? item.getMinStockLevel().doubleValue() : 0);
            row.createCell(col++).setCellValue(item.getReorderQuantity() != null ? item.getReorderQuantity().doubleValue() : 0);
            row.createCell(col++).setCellValue(item.getStockStatus() != null ? item.getStockStatus().name() : "");
            row.createCell(col++).setCellValue(safe(item.getBatchLot()));
            row.createCell(col++).setCellValue(safe(item.getSerialNumber()));
            row.createCell(col).setCellValue(item.getExpiresAt() != null ? item.getExpiresAt().toString() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<InventoryItemDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (InventoryItemDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    String.valueOf(item.getProductId() != null ? item.getProductId() : 0),
                    safe(item.getProductCode()),
                    safe(item.getProductBarcode()),
                    safe(item.getProductName()),
                    String.valueOf(item.getWarehouseLocationId() != null ? item.getWarehouseLocationId() : 0),
                    safe(item.getWarehouseLocationName()),
                    String.valueOf(item.getSupplierId() != null ? item.getSupplierId() : 0),
                    String.valueOf(item.getCurrentStock() != null ? item.getCurrentStock() : 0),
                    String.valueOf(item.getReservedQuantity() != null ? item.getReservedQuantity() : 0),
                    String.valueOf(item.getAvailableQuantity() != null ? item.getAvailableQuantity() : 0),
                    safe(item.getUnitOfMeasure()),
                    String.valueOf(item.getMinStockLevel() != null ? item.getMinStockLevel() : 0),
                    String.valueOf(item.getReorderQuantity() != null ? item.getReorderQuantity() : 0),
                    item.getStockStatus() != null ? item.getStockStatus().name() : "",
                    safe(item.getBatchLot()),
                    safe(item.getSerialNumber()),
                    item.getExpiresAt() != null ? item.getExpiresAt().toString() : ""
            });
        }
        return InventoryExportSupport.writeTabularPdf("Stock Levels", "INV-STK",
                "Inventory stock level export", HEADERS, rows, true);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportSummary importInventoryItemFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;
        try (InputStream bomFree = BOMInputStream.builder().setInputStream(csvInputStream).get();
             Reader reader = new InputStreamReader(bomFree, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            List<CSVRecord> records = csvParser.getRecords();
            total = records.size();
            for (CSVRecord record : records) {
                try {
                    CreateInventoryItemRequest request = new CreateInventoryItemRequest();
                    String productCode = csvString(record, "PRODUCT_CODE", "PRODUCT_C", "SKU", "SKU_CODE");
                    String barcode = csvString(record, "BARCODE", "BAR_CODE", "EAN", "UPC");
                    Long productId = csvLong(record, "PRODUCT_ID");
                    if (productId == null && productCode != null) {
                        productId = resolveProductIdByCode(productCode);
                    }
                    if (productId == null && barcode != null) {
                        productId = resolveProductIdByBarcode(barcode);
                    }
                    if (productId == null) {
                        failed++;
                        String codeHint = productCode != null ? productCode
                                : barcode != null ? barcode
                                : "(missing PRODUCT_ID, PRODUCT_CODE, or BARCODE)";
                        errors.add("Row " + record.getRecordNumber() + ": Product not found for " + codeHint);
                        continue;
                    }
                    request.setProductId(productId);
                    Long warehouseLocationId = csvLong(record,
                            "WAREHOUSE_LOCATION_ID", "WAREHOUSE_ID", "WAREHOUSE", "WAREHOUSE_LOC");
                    if (warehouseLocationId == null) {
                        failed++;
                        errors.add("Row " + record.getRecordNumber() + ": Warehouse location is missing or invalid");
                        continue;
                    }
                    request.setWarehouseLocationId(warehouseLocationId);
                    request.setSupplierId(csvLong(record, "SUPPLIER_ID"));
                    BigDecimal currentStock = csvDecimal(record,
                            "CURRENT_STOCK", "QUANTITY", "QTY", "ON_HAND", "STOCK");
                    if (currentStock == null) {
                        failed++;
                        errors.add("Row " + record.getRecordNumber() + ": Stock quantity is missing or invalid");
                        continue;
                    }
                    request.setCurrentStock(currentStock);
                    request.setMinStockLevel(csvDecimal(record,
                            "MIN_STOCK_LEVEL", "MIN_STOCK", "MINIMUM_STOCK", "MIN_STOCK_LVL"));
                    request.setReorderQuantity(csvDecimal(record,
                            "REORDER_QUANTITY", "REORDER_POINT", "REORDER_QTY", "REORDER_P"));
                    request.setBatchLot(csvString(record, "BATCH_LOT"));
                    request.setSerialNumber(csvString(record, "SERIAL_NUMBER"));
                    String expiresAt = csvString(record, "EXPIRES_AT");
                    request.setExpiresAt(expiresAt != null ? LocalDate.parse(expiresAt) : null);

                    InventoryItemResponse response = self.create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        String rowError = response.getErrorMessages() != null && !response.getErrorMessages().isEmpty()
                                ? String.join("; ", response.getErrorMessages())
                                : response.getMessage();
                        errors.add("Row " + record.getRecordNumber() + ": " + rowError);
                    }
                } catch (Exception e) {
                    entityManager.clear();
                    failed++;
                    errors.add("Row " + record.getRecordNumber() + ": Unexpected error - " + e.getMessage());
                }
            }
        }
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = isSuccess
                ? "Import completed successfully. " + success + " out of " + total + " inventory items imported."
                : "Import failed. No inventory items were imported.";
        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    /**
     * Create initial stock (opening balance) for a product at a warehouse
     * This is a one-time operation that sets the starting inventory level
     */
    @Override
    @Transactional
    public InventoryItemResponse createInitialStock(CreateInitialStockRequest request, Locale locale, String username) {

        String message;

        // STEP 1: Validate request
        ValidatorDto validatorDto = validator.isCreateInitialStockRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_INITIAL_STOCK_INVALID_REQUEST.getCode(), new String[]{}, locale
            );
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        // STEP 2: Validate product exists
        Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(request.getProductId(),
                EntityStatus.DELETED);

        if (productOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        // STEP 3: Validate warehouse location exists
        Optional<WarehouseLocation> locationOpt = warehouseLocationRepository.findByIdAndEntityStatusNot(
                request.getWarehouseLocationId(),
                EntityStatus.DELETED
        );
        if (locationOpt.isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(),
                    new String[]{},
                    locale
            );

            return buildResponse(404, false, message, null, null, null);
        }

        // STEP 4: Check if inventory item already exists (idempotency)
        Optional<InventoryItem> existingItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        request.getProductId(),
                        request.getWarehouseLocationId(),
                        EntityStatus.DELETED
                );

        Long supplierId = resolveSupplierId(request.getSupplierId(), productOpt.get(), locationOpt.get());
        if (supplierId == null) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_PRODUCT_SUPPLIER_ID_INVALID.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        InventoryItem inventoryItem;

        if (existingItemOpt.isPresent()) {
            // Item already exists - check if it already has stock
            inventoryItem = existingItemOpt.get();

            if (inventoryItem.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
                message = messageService.getMessage(
                        I18Code.MESSAGE_INITIAL_STOCK_ALREADY_EXISTS.getCode(),
                        new String[]{
                                inventoryItem.getProduct().getName(),
                                inventoryItem.getWarehouseLocation().getName(),
                                inventoryItem.getCurrentStock().toString()
                        },
                        locale
                );
                return buildResponse(400, false, message, null, null,
                        List.of("Initial stock already set for this product at this warehouse"));
            }

            // Stock is zero - allow setting initial stock and update optional fields from request
            inventoryItem.setSupplierId(supplierId);
            inventoryItem.setMinStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : inventoryItem.getMinStockLevel());
            inventoryItem.setReorderQuantity(request.getReorderQuantity() != null ? request.getReorderQuantity() : inventoryItem.getReorderQuantity());
            if (request.getBatchLot() != null) {
                inventoryItem.setBatchLot(request.getBatchLot());
            }
            if (request.getSerialNumber() != null) {
                inventoryItem.setSerialNumber(request.getSerialNumber());
            }
            if (request.getExpiresAt() != null) {
                inventoryItem.setExpiresAt(request.getExpiresAt());
            }

            log.info("Inventory item exists but has zero stock - setting initial stock and updating fields for product {} at warehouse {}",
                    request.getProductId(), request.getWarehouseLocationId());
        } else {
            // STEP 5: Create new inventory item
            inventoryItem = new InventoryItem();
            inventoryItem.setProduct(productOpt.get());
            inventoryItem.setWarehouseLocation(locationOpt.get());
            inventoryItem.setSupplierId(supplierId);
            inventoryItem.setCreatedByUserId(request.getCreatedByUserId());

            // Initialize stock and cost fields to zero (will be updated in STEP 6)
            inventoryItem.setCurrentStock(BigDecimal.ZERO);
            inventoryItem.setReservedQuantity(BigDecimal.ZERO);
            inventoryItem.setAverageCost(BigDecimal.ZERO);
            inventoryItem.setTotalCost(BigDecimal.ZERO);
            inventoryItem.setUnitCost(BigDecimal.ZERO);
            inventoryItem.setLastPurchaseCost(BigDecimal.ZERO);

            // Set inventory management fields from request (optional fields)
            inventoryItem.setMinStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : BigDecimal.ZERO);
            inventoryItem.setReorderQuantity(request.getReorderQuantity() != null ? request.getReorderQuantity() : BigDecimal.ZERO);
            inventoryItem.setBatchLot(request.getBatchLot());
            inventoryItem.setSerialNumber(request.getSerialNumber());
            inventoryItem.setExpiresAt(request.getExpiresAt());

            inventoryItem = inventoryItemServiceAuditable.create(inventoryItem, locale, username);
            log.info("Created new inventory item for initial stock: product={}, warehouse={}, supplier={}",
                    request.getProductId(), request.getWarehouseLocationId(), request.getSupplierId());
        }

        // STEP 6: Set initial stock values using WAC
        BigDecimal quantity = request.getQuantity();
        BigDecimal unitCost = request.getUnitCost();
        BigDecimal totalCost = quantity.multiply(unitCost);

        inventoryItem.setCurrentStock(quantity);
        inventoryItem.setTotalCost(totalCost);
        inventoryItem.setAverageCost(unitCost);
        inventoryItem.setUnitCost(unitCost);
        inventoryItem.setLastPurchaseCost(unitCost);

        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);

        // STEP 7: Create stock transaction history for audit trail
        StockTransactionHistory transaction = new StockTransactionHistory();
        transaction.setInventoryItem(savedItem);
        transaction.setTransactionType(TransactionType.STOCK_IN);
        transaction.setQuantityChange(quantity);
        transaction.setUnitCost(unitCost);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setWarehouseLocation(locationOpt.get());
        transaction.setPerformedByUserId(request.getCreatedByUserId());
        transaction.setReferenceDocumentId(savedItem.getId());
        transaction.setReferenceDocumentType(ReferenceDocumentType.OPENING_BALANCE);
        transaction.setReason(request.getNotes() != null ? request.getNotes() : "Initial stock / Opening balance");

        stockTransactionHistoryServiceAuditable.create(transaction, locale, username);

        // STEP 8: Publish event (optional - for analytics/reporting)
        publishInitialStockCreatedEvent(savedItem, quantity, unitCost);

        // STEP 9: Build successful response
        InventoryItemDto dto = mapToDto(savedItem);
        message = messageService.getMessage(
                I18Code.MESSAGE_INITIAL_STOCK_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale
        );

        log.info("Successfully created initial stock: product={}, warehouse={}, quantity={}, unitCost={}, totalCost={}",
                request.getProductId(), request.getWarehouseLocationId(), quantity, unitCost, totalCost);

        return buildResponse(201, true, message, dto, null, null);
    }

    /**
     * Create initial stock in bulk
     */
    @Override
    @Transactional
    public InventoryItemResponse createInitialStockBulk(List<CreateInitialStockRequest> requests,
                                                        Locale locale, String username) {
        String message;
        List<InventoryItemDto> successfulItems = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        log.info("Starting bulk initial stock creation for {} items", requests.size());

        for (int i = 0; i < requests.size(); i++) {
            CreateInitialStockRequest request = requests.get(i);
            try {
                InventoryItemResponse response = createInitialStock(request, locale, username);

                if (response.isSuccess()) {
                    successfulItems.add(response.getInventoryItemDto());
                    successCount++;
                } else {
                    failureCount++;
                    String error = String.format("Row %d (Product ID: %d, Warehouse ID: %d): %s",
                            i + 1,
                            request.getProductId(),
                            request.getWarehouseLocationId(),
                            response.getMessage());
                    errors.add(error);
                }
            } catch (Exception e) {
                failureCount++;
                String error = String.format("Row %d (Product ID: %d, Warehouse ID: %d): %s",
                        i + 1,
                        request.getProductId(),
                        request.getWarehouseLocationId(),
                        e.getMessage());
                errors.add(error);
                log.error("Failed to create initial stock for row {}: {}", i + 1, e.getMessage());
            }
        }

        // Build summary response
        boolean overallSuccess = successCount > 0;
        int statusCode = overallSuccess ? 200 : 400;

        message = String.format("Bulk initial stock creation completed: %d succeeded, %d failed out of %d total",
                successCount, failureCount, requests.size());

        log.info("Bulk initial stock creation summary: {} succeeded, {} failed", successCount, failureCount);

        return buildResponse(statusCode, overallSuccess, message, null, successfulItems,
                errors.isEmpty() ? null : errors);
    }

    private void createStockTransactionHistory(InventoryItem inventoryItem, BigDecimal quantityChange,
                                               BigDecimal unitCost, Long userId, Long referenceDocumentId,
                                               ReferenceDocumentType referenceDocumentType, String reason,
                                               Locale locale, String username) {
        StockTransactionHistory transaction = new StockTransactionHistory();
        transaction.setInventoryItem(inventoryItem);
        transaction.setTransactionType(quantityChange.compareTo(BigDecimal.ZERO) >= 0 ?
                TransactionType.STOCK_IN : TransactionType.STOCK_OUT);
        transaction.setQuantityChange(quantityChange);
        transaction.setUnitCost(unitCost);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setWarehouseLocation(inventoryItem.getWarehouseLocation());
        transaction.setPerformedByUserId(userId);
        transaction.setReferenceDocumentId(referenceDocumentId);
        transaction.setReferenceDocumentType(referenceDocumentType);
        transaction.setReason(reason != null ? reason : "Stock adjustment");

        try {
            stockTransactionHistoryServiceAuditable.create(transaction, locale, username);
        } catch (Exception e) {
            log.error("Failed to create transaction history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create transaction history: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to fetch supplier details
     */
    private projectlx.co.zw.shared_library.utils.responses.OrganizationResponse getSupplierDetails(Long supplierId) {
        projectlx.co.zw.shared_library.utils.responses.OrganizationResponse supplierResponse = new projectlx.co.zw.shared_library.utils.responses.OrganizationResponse();
        
        try {
            supplierResponse = organizationServiceClient.findById(supplierId, Locale.ENGLISH);
        } catch (Exception e) {
            log.error("Failed to fetch supplier for supplier ID: {}. Error: {}", supplierId, e.getMessage());
        }
        
        return supplierResponse;
    }

    /**
     * Send low stock alert notification
     */
    private void sendLowStockAlert(InventoryItem inventoryItem) {
        try {
            // Send it to procurement team and suppliers
            sendLowStockAlertToProcurement(inventoryItem);
            sendLowStockAlertToSupplier(inventoryItem);
            
            log.info("Successfully sent low stock alert for product {} at warehouse {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName());
                    
        } catch (Exception e) {
            log.error("Failed to send low stock alert for product {} at warehouse {}. Error: {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName(), e.getMessage());
        }
    }

    /**
     * Send low stock alert to procurement team
     */
    private void sendLowStockAlertToProcurement(InventoryItem inventoryItem) {
        try {
            Map<String, Object> procurementData = Map.of(
                    "productName", inventoryItem.getProduct().getName(),
                    "productId", inventoryItem.getProduct().getId(),
                    "warehouseLocation", inventoryItem.getWarehouseLocation().getName(),
                    "currentStock", inventoryItem.getCurrentStock().toString(),
                    "minStockLevel", inventoryItem.getMinStockLevel() != null ? inventoryItem.getMinStockLevel().toString() : "Not Set",
                    "reorderQuantity", inventoryItem.getReorderQuantity() != null ? inventoryItem.getReorderQuantity().toString() : "Not Set",
                    "supplierId", inventoryItem.getSupplierId() != null ? inventoryItem.getSupplierId() : 0L,
                    "alertType", "LOW_STOCK"
            );

            NotificationRequest.Recipient procurementRecipient = new NotificationRequest.Recipient(
                    null, // Internal recipient - will be handled by notification service
                    null,
                    null,
                    null
            );
            
            NotificationRequest procurementNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "STOCK_LOW_PROCUREMENT_EMAIL", // Procurement team template
                    procurementRecipient,
                    procurementData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", procurementNotification);
            
            log.info("Sent low stock alert to procurement team for product: {}", inventoryItem.getProduct().getName());
        } catch (Exception e) {
            log.error("Failed to send low stock alert to procurement team for product: {}. Error: {}", 
                    inventoryItem.getProduct().getName(), e.getMessage());
        }
    }

    /**
     * Send low stock alert to supplier
     */
    private void sendLowStockAlertToSupplier(InventoryItem inventoryItem) {
        try {
            if (inventoryItem.getSupplierId() == null) {
                log.info("No supplier ID for product {}, skipping supplier notification", inventoryItem.getProduct().getName());
                return;
            }

            Map<String, Object> supplierData = Map.of(
                    "productName", inventoryItem.getProduct().getName(),
                    "productId", inventoryItem.getProduct().getId(),
                    "warehouseLocation", inventoryItem.getWarehouseLocation().getName(),
                    "currentStock", inventoryItem.getCurrentStock().toString(),
                    "minStockLevel", inventoryItem.getMinStockLevel() != null ? inventoryItem.getMinStockLevel().toString() : "Not Set",
                    "reorderQuantity", inventoryItem.getReorderQuantity() != null ? inventoryItem.getReorderQuantity().toString() : "Not Set",
                    "alertType", "LOW_STOCK_SUPPLIER"
            );

            // Get supplier contact information
            projectlx.co.zw.shared_library.utils.responses.OrganizationResponse supplierResponse = getSupplierDetails(inventoryItem.getSupplierId());

            if (supplierResponse != null && supplierResponse.getOrganizationDto() != null) {
                
                // Send email to supplier
                if (supplierResponse.getOrganizationDto().getEmail() != null && 
                        !supplierResponse.getOrganizationDto().getEmail().isBlank()) {
                    
                    NotificationRequest.Recipient supplierEmailRecipient = new NotificationRequest.Recipient(
                            null, // External recipient
                            supplierResponse.getOrganizationDto().getEmail(),
                            null,
                            null
                    );
                    
                    NotificationRequest supplierEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "STOCK_LOW_SUPPLIER_EMAIL", // Supplier-specific template
                            supplierEmailRecipient,
                            supplierData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierEmailNotification);
                }
            }
            
            log.info("Sent low stock alert to supplier for product: {}", inventoryItem.getProduct().getName());
        } catch (Exception e) {
            log.error("Failed to send low stock alert to supplier for product: {}. Error: {}", 
                    inventoryItem.getProduct().getName(), e.getMessage());
        }
    }

    /**
     * Send stock out alert notification
     */
    private void sendStockOutAlert(InventoryItem inventoryItem) {
        try {
            sendStockOutAlertToSalesTeam(inventoryItem);
            sendStockOutAlertToManagement(inventoryItem);
            
            log.info("Successfully sent stock out alert for product {} at warehouse {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName());
                    
        } catch (Exception e) {
            log.error("Failed to send stock out alert for product {} at warehouse {}. Error: {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName(), e.getMessage());
        }
    }

    /**
     * Send stock out alert to sales team
     */
    private void sendStockOutAlertToSalesTeam(InventoryItem inventoryItem) {
        try {
            Map<String, Object> salesData = Map.of(
                    "productName", inventoryItem.getProduct().getName(),
                    "productId", inventoryItem.getProduct().getId(),
                    "warehouseLocation", inventoryItem.getWarehouseLocation().getName(),
                    "currentStock", inventoryItem.getCurrentStock().toString(),
                    "alertType", "STOCK_OUT",
                    "message", "Product is now out of stock - stop taking new orders"
            );

            NotificationRequest.Recipient salesRecipient = new NotificationRequest.Recipient(
                    null, // Internal recipient
                    null,
                    null,
                    null
            );
            
            NotificationRequest salesNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "STOCK_OUT_SALES_TEAM_EMAIL", 
                    salesRecipient,
                    salesData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", salesNotification);
            
            log.info("Sent stock out alert to sales team for product: {}", inventoryItem.getProduct().getName());
        } catch (Exception e) {
            log.error("Failed to send stock out alert to sales team for product: {}. Error: {}", 
                    inventoryItem.getProduct().getName(), e.getMessage());
        }
    }

    /**
     * Send stock out alert to management
     */
    private void sendStockOutAlertToManagement(InventoryItem inventoryItem) {
        try {
            Map<String, Object> managementData = Map.of(
                    "productName", inventoryItem.getProduct().getName(),
                    "productId", inventoryItem.getProduct().getId(),
                    "warehouseLocation", inventoryItem.getWarehouseLocation().getName(),
                    "currentStock", inventoryItem.getCurrentStock().toString(),
                    "alertType", "STOCK_OUT_MANAGEMENT",
                    "impact", "Critical - Product unavailable for sales"
            );

            NotificationRequest managementNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "STOCK_OUT_MANAGEMENT_EMAIL",
                    new NotificationRequest.Recipient(null, null, null, null),
                    managementData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", managementNotification);
        } catch (Exception e) {
            log.error("Failed to send stock out alert to management for product: {}. Error: {}", 
                    inventoryItem.getProduct().getName(), e.getMessage());
        }
    }

    /**
     * Send stock replenishment notification
     */
    private void sendStockReplenishmentNotification(InventoryItem inventoryItem, BigDecimal quantityAdded) {
        try {
            sendStockReplenishmentToSalesTeam(inventoryItem, quantityAdded);
            
            log.info("Successfully sent stock replenishment notification for product {} at warehouse {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName());
                    
        } catch (Exception e) {
            log.error("Failed to send stock replenishment notification for product {} at warehouse {}. Error: {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName(), e.getMessage());
        }
    }

    /**
     * Send stock replenishment notification to sales team
     */
    private void sendStockReplenishmentToSalesTeam(InventoryItem inventoryItem, BigDecimal quantityAdded) {
        try {
            Map<String, Object> salesData = Map.of(
                    "productName", inventoryItem.getProduct().getName(),
                    "productId", inventoryItem.getProduct().getId(),
                    "warehouseLocation", inventoryItem.getWarehouseLocation().getName(),
                    "currentStock", inventoryItem.getCurrentStock().toString(),
                    "quantityAdded", quantityAdded.toString(),
                    "alertType", "STOCK_REPLENISHMENT",
                    "message", "Product is back in stock and available for sales"
            );

            NotificationRequest salesNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "STOCK_REPLENISHMENT_SALES_TEAM_EMAIL",
                    new NotificationRequest.Recipient(null, null, null, null),
                    salesData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", salesNotification);
            
            log.info("Sent stock replenishment notification to sales team for product: {}", inventoryItem.getProduct().getName());
        } catch (Exception e) {
            log.error("Failed to send stock replenishment notification to sales team for product: {}. Error: {}", 
                    inventoryItem.getProduct().getName(), e.getMessage());
        }
    }

    /**
     * Send stock adjustment notification
     */
    private void sendStockAdjustmentNotification(InventoryItem inventoryItem, BigDecimal quantityChange, String reason) {
        try {
            sendStockAdjustmentToWarehouseManagers(inventoryItem, quantityChange, reason);
            
            log.info("Successfully sent stock adjustment notification for product {} at warehouse {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName());
                    
        } catch (Exception e) {
            log.error("Failed to send stock adjustment notification for product {} at warehouse {}. Error: {}", 
                    inventoryItem.getProduct().getName(), inventoryItem.getWarehouseLocation().getName(), e.getMessage());
        }
    }

    /**
     * Send stock adjustment notification to warehouse managers
     */
    private void sendStockAdjustmentToWarehouseManagers(InventoryItem inventoryItem, BigDecimal quantityChange, String reason) {
        try {
            Map<String, Object> warehouseData = Map.of(
                    "productName", inventoryItem.getProduct().getName(),
                    "productId", inventoryItem.getProduct().getId(),
                    "warehouseLocation", inventoryItem.getWarehouseLocation().getName(),
                    "currentStock", inventoryItem.getCurrentStock().toString(),
                    "quantityChange", quantityChange.toString(),
                    "adjustmentType", quantityChange.compareTo(BigDecimal.ZERO) >= 0 ? "INCREASE" : "DECREASE",
                    "reason", reason != null ? reason : "Stock adjustment",
                    "alertType", "STOCK_ADJUSTMENT"
            );

            NotificationRequest warehouseNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "STOCK_ADJUSTMENT_WAREHOUSE_INTERNAL",
                    new NotificationRequest.Recipient(null, null, null, null),
                    warehouseData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", warehouseNotification);
            
            log.info("Sent stock adjustment notification to warehouse managers for product: {}", inventoryItem.getProduct().getName());
        } catch (Exception e) {
            log.error("Failed to send stock adjustment notification to warehouse managers for product: {}. Error: {}", 
                    inventoryItem.getProduct().getName(), e.getMessage());
        }
    }


    /**
     * Maps an inventory item entity to a DTO, resolving product and warehouse display fields.
     */
    private InventoryItemDto mapToDto(InventoryItem item) {
        InventoryItemDto dto = new InventoryItemDto();
        dto.setId(item.getId());
        dto.setSupplierId(item.getSupplierId());
        dto.setCurrentStock(item.getCurrentStock());
        dto.setReservedQuantity(item.getReservedQuantity());
        dto.setTotalCost(item.getTotalCost());
        dto.setUnitCost(resolveCapturedUnitCost(item));
        dto.setMinStockLevel(item.getMinStockLevel());
        dto.setReorderQuantity(item.getReorderQuantity());
        dto.setBatchLot(item.getBatchLot());
        dto.setSerialNumber(item.getSerialNumber());
        dto.setCreatedByUserId(item.getCreatedByUserId());
        dto.setUpdatedByUserId(item.getUpdatedByUserId());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setExpiresAt(item.getExpiresAt());
        dto.setEntityStatus(item.getEntityStatus());

        Product product = item.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setProductCode(product.getProductCode());
            dto.setProductBarcode(product.getBarcode());
            if (product.getUnitOfMeasure() != null) {
                dto.setUnitOfMeasure(product.getUnitOfMeasure().name());
            }
        }

        WarehouseLocation warehouse = item.getWarehouseLocation();
        if (warehouse != null) {
            dto.setWarehouseLocationId(warehouse.getId());
            dto.setWarehouseLocationName(warehouse.getName());
        }

        dto.setAvailableQuantity(item.getAvailableQuantity());
        dto.setStockStatus(resolveStockStatus(item));

        return dto;
    }

    private BigDecimal resolveCapturedUnitCost(InventoryItem item) {
        if (item.getUnitCost() != null && item.getUnitCost().compareTo(BigDecimal.ZERO) > 0) {
            return item.getUnitCost();
        }
        return item.getAverageCost();
    }

    private StockLevelStatus resolveStockStatus(InventoryItem item) {
        BigDecimal current = item.getCurrentStock() != null ? item.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal reserved = item.getReservedQuantity() != null ? item.getReservedQuantity() : BigDecimal.ZERO;
        BigDecimal min = item.getMinStockLevel() != null ? item.getMinStockLevel() : BigDecimal.ZERO;

        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            return StockLevelStatus.OUT_OF_STOCK;
        }
        if (min.compareTo(BigDecimal.ZERO) > 0 && current.compareTo(min) <= 0) {
            return StockLevelStatus.LOW_STOCK;
        }
        if (current.subtract(reserved).compareTo(BigDecimal.ZERO) <= 0) {
            return StockLevelStatus.FULLY_RESERVED;
        }
        return StockLevelStatus.IN_STOCK;
    }

    /**
     * Publish initial stock created event to RabbitMQ
     */
    private void publishInitialStockCreatedEvent(InventoryItem inventoryItem,
                                                 BigDecimal quantity,
                                                 BigDecimal unitCost) {
        try {
            Map<String, Object> eventData = Map.ofEntries(
                    Map.entry("inventoryItemId", inventoryItem.getId()),
                    Map.entry("productId", inventoryItem.getProduct().getId()),
                    Map.entry("productName", inventoryItem.getProduct().getName()),
                    Map.entry("warehouseId", inventoryItem.getWarehouseLocation().getId()),
                    Map.entry("warehouseName", inventoryItem.getWarehouseLocation().getName()),
                    Map.entry("quantity", quantity.toString()),
                    Map.entry("unitCost", unitCost.toString()),
                    Map.entry("totalCost", inventoryItem.getTotalCost().toString()),
                    Map.entry("averageCost", inventoryItem.getAverageCost().toString()),
                    Map.entry("eventType", "INITIAL_STOCK_CREATED"),
                    Map.entry("timestamp", LocalDateTime.now().toString())
            );

            rabbitTemplate.convertAndSend(
                    "inventory.exchange",
                    "inventory.initial_stock.created",
                    eventData
            );

            log.info("Published initial_stock.created event for inventory item {}", inventoryItem.getId());
        } catch (Exception e) {
            log.error("Failed to publish initial_stock.created event for inventory item {}: {}",
                    inventoryItem.getId(), e.getMessage());
            // Don't throw - event failure shouldn't rollback transaction
        }
    }

    private Long resolveProductIdByCode(String productCode) {
        return productRepository.lookupByProductCodeTrimmedAndEntityStatusNot(productCode, EntityStatus.DELETED)
                .or(() -> productRepository.lookupByProductCodeAndEntityStatusNot(productCode, EntityStatus.DELETED))
                .map(Product::getId)
                .orElse(null);
    }

    private Long resolveProductIdByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        String trimmed = barcode.trim();
        return productRepository.lookupByBarcodeTrimmedAndEntityStatusNot(trimmed, EntityStatus.DELETED)
                .or(() -> productRepository.lookupByBarcodeAndEntityStatusNot(trimmed, EntityStatus.DELETED))
                .map(Product::getId)
                .orElse(null);
    }

    private static Long resolveSupplierId(Long requestedSupplierId, Product product, WarehouseLocation warehouse) {
        if (requestedSupplierId != null && requestedSupplierId > 0) {
            return requestedSupplierId;
        }
        if (product != null && product.getSupplierId() != null && product.getSupplierId() > 0) {
            return product.getSupplierId();
        }
        if (warehouse != null && warehouse.getSupplierId() != null && warehouse.getSupplierId() > 0) {
            return warehouse.getSupplierId();
        }
        return null;
    }

    private static String normalizeCsvHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim().toUpperCase(Locale.ROOT);
    }

    private static String csvString(CSVRecord record, String... headerNames) {
        for (String headerName : headerNames) {
            if (record.isMapped(headerName)) {
                String value = record.get(headerName);
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        Map<String, Integer> headerMap = record.getParser().getHeaderMap();
        if (headerMap == null || headerMap.isEmpty()) {
            return null;
        }
        for (String headerName : headerNames) {
            String normalizedTarget = normalizeCsvHeader(headerName);
            for (String actualHeader : headerMap.keySet()) {
                if (normalizeCsvHeader(actualHeader).equals(normalizedTarget)) {
                    String value = record.get(actualHeader);
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                }
            }
        }
        for (String headerName : headerNames) {
            String normalizedTarget = normalizeCsvHeader(headerName);
            if (normalizedTarget.length() < 4) {
                continue;
            }
            for (String actualHeader : headerMap.keySet()) {
                String normalizedActual = normalizeCsvHeader(actualHeader);
                if (normalizedActual.length() < 4) {
                    continue;
                }
                if (normalizedTarget.startsWith(normalizedActual) || normalizedActual.startsWith(normalizedTarget)) {
                    String value = record.get(actualHeader);
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                }
            }
        }
        return null;
    }

    private static Long csvLong(CSVRecord record, String... headerNames) {
        String value = csvString(record, headerNames);
        return value != null ? Long.parseLong(value) : null;
    }

    private static BigDecimal csvDecimal(CSVRecord record, String... headerNames) {
        String value = csvString(record, headerNames);
        return value != null ? new BigDecimal(value) : null;
    }
}