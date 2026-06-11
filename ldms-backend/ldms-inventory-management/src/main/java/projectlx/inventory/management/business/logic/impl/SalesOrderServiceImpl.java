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
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.SalesOrderServiceAuditable;
import projectlx.inventory.management.business.logic.api.InventoryAllocationService;
import projectlx.inventory.management.business.logic.api.SalesOrderService;
import projectlx.inventory.management.business.logic.api.SalesOrderStatusManager;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.validator.api.SalesOrderServiceValidator;
import projectlx.inventory.management.clients.OrganizationServiceClient;
import projectlx.inventory.management.clients.UserManagementServiceClient;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.SalesOrderRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.InventoryReservationRepository;
import projectlx.inventory.management.model.InventoryReservation;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.repository.specification.SalesOrderSpecification;
import projectlx.inventory.management.utils.dtos.SalesOrderDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateSalesOrderRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderRequest;
import projectlx.inventory.management.utils.requests.NotificationRequest;
import projectlx.inventory.management.utils.requests.SalesOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.FulfillSalesOrderRequest;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;
import projectlx.inventory.management.business.logic.api.IdempotencyService;
import projectlx.inventory.management.model.IdempotencyKey;
import projectlx.inventory.management.model.IdempotencyOperation;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SalesOrderServiceAuditable salesOrderServiceAuditable;
    private final InventoryItemService inventoryItemService;
    private final InventoryAllocationService inventoryAllocationService;
    private final SalesOrderStatusManager salesOrderStatusManager;
    private final SalesOrderServiceValidator validator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final RabbitTemplate rabbitTemplate;
    private final OrganizationServiceClient organizationServiceClient;
    private final UserManagementServiceClient userManagementServiceClient;
    private final IdempotencyService idempotencyService;
    private final InventoryReservationRepository inventoryReservationRepository;

    private static final String[] HEADERS = {"ID", "SALES_ORDER_NUMBER", "CUSTOMER_ID", "STATUS", "ORDER_DATE",
            "EXPECTED_DELIVERY_DATE", "DELIVERED_DATE", "TOTAL_AMOUNT", "NOTES"};
    private static final String[] CSV_HEADERS = {"CUSTOMER_ID", "ORDER_DATE", "EXPECTED_DELIVERY_DATE", "NOTES"};

    @Override
    @Transactional
    public SalesOrderResponse create(CreateSalesOrderRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isCreateSalesOrderRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_SALES_ORDER_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(request.getCustomerId());
        salesOrder.setStatus(request.getStatus() != null ? request.getStatus() : SalesOrderStatus.PENDING);
        if (request.getOrderDate() != null) salesOrder.setOrderDate(request.getOrderDate());
        if (request.getExpectedDeliveryDate() != null) salesOrder.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        salesOrder.setNotes(request.getNotes());
        salesOrder.setCreatedByUserId(request.getCreatedByUserId());
        salesOrder.setPaymentTerm(request.getPaymentTerm());
        salesOrder.setSalesOrderNumber(generateSalesOrderNumber());

        List<SalesOrderLine> salesOrderLineList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (request.getLines() != null && !request.getLines().isEmpty()) {

            for (CreateSalesOrderRequest.SalesOrderLineRequest lineReq : request.getLines()) {

                Product product = productRepository.findByIdAndEntityStatusNot(lineReq.getProductId(),
                        EntityStatus.DELETED).orElse(null);

                if (product == null) {
                    continue;
                }

                SalesOrderLine salesOrderLine = new SalesOrderLine();
                salesOrderLine.setSalesOrder(salesOrder);
                salesOrderLine.setProduct(product);
                salesOrderLine.setUnitOfMeasure(lineReq.getUnitOfMeasure());
                salesOrderLine.setQuantity(lineReq.getQuantity());
                salesOrderLine.setUnitPrice(lineReq.getUnitPrice());
                salesOrderLine.setCreatedByUserId(request.getCreatedByUserId());

                BigDecimal lineTotal = lineReq.getQuantity().multiply(lineReq.getUnitPrice());
                salesOrderLine.setTotalPrice(lineTotal);
                totalAmount = totalAmount.add(lineTotal);

                salesOrderLineList.add(salesOrderLine);
            }
        }

        salesOrder.setSalesOrderLines(salesOrderLineList);
        salesOrder.setTotalAmount(totalAmount);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesOrder saved = salesOrderServiceAuditable.create(salesOrder, locale, username);

        // Send sales order created notification
        sendSalesOrderCreatedNotification(saved);

        SalesOrderDto salesOrderDto = modelMapper.map(saved, SalesOrderDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, message, salesOrderDto, null, null);
    }

    @Override
    public SalesOrderResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<SalesOrder> salesOrder = findSalesOrderForUser(id, username, locale);

        if (salesOrder.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesOrderDto dto = modelMapper.map(salesOrder.get(), SalesOrderDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public SalesOrderResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<SalesOrder> salesOrderList;
        if (isSystemUser(username)) {
            salesOrderList = salesOrderRepository.findAll().stream()
                    .filter(order -> order.getEntityStatus() != EntityStatus.DELETED)
                    .collect(Collectors.toList());
        } else {
            Long organizationId = resolveOrganizationId(username, locale);
            if (organizationId == null) {
                message = "Organization could not be resolved for user";
                return buildResponse(400, false, message, null, null, null);
            }
            salesOrderList = salesOrderRepository.findBySupplierOrganizationIdAndEntityStatusNot(
                    organizationId, EntityStatus.DELETED);
        }

        if (salesOrderList.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);
            return buildResponse(200, true, message, null, List.of(), null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<SalesOrderDto> salesOrderDtoList = salesOrderList.stream()
                .map(order -> modelMapper.map(order, SalesOrderDto.class))
                .collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, null, salesOrderDtoList, null);
    }

    @Override
    @Transactional
    public SalesOrderResponse update(EditSalesOrderRequest request, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = validator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_SALES_ORDER_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<SalesOrder> existingSalesOrder = findSalesOrderForUser(
                request.getSalesOrderId(), username, locale);

        if (existingSalesOrder.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrder salesOrderToEdit = existingSalesOrder.get();

        // Check if order can be edited
        if (salesOrderToEdit.getStatus() == SalesOrderStatus.FULFILLED ||
                salesOrderToEdit.getStatus() == SalesOrderStatus.CANCELLED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_EDITABLE.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // Handle status transitions with proper validation
        if (request.getStatus() != null) {
            SalesOrderStatus current = salesOrderToEdit.getStatus();
            SalesOrderStatus target = request.getStatus();

            if (!salesOrderStatusManager.canTransition(current, target)) {
                message = messageService.getMessage(I18Code.MESSAGE_INVALID_STATUS_TRANSITION.getCode(),
                        new String[]{current.name(), target.name()}, locale);
                return buildResponse(400, false, message, null, null, null);
            }

            try {
                Long warehouseId = getDefaultWarehouseId(salesOrderToEdit.getCustomerId());
                salesOrderStatusManager.transition(salesOrderToEdit, target, warehouseId, username, locale);
            } catch (IllegalStateException e) {
                message = "Cannot update order status: " + e.getMessage();
                return buildResponse(400, false, message, null, null, null);
            }

            // Send appropriate notifications based on status change
            if (target == SalesOrderStatus.CONFIRMED) {
                sendSalesOrderConfirmedNotification(salesOrderToEdit);
            } else if (target == SalesOrderStatus.SHIPPED || target == SalesOrderStatus.PARTIALLY_SHIPPED) {
                sendSalesOrderShippedNotification(salesOrderToEdit);
            } else if (target == SalesOrderStatus.DELIVERED) {
                sendSalesOrderDeliveredNotification(salesOrderToEdit);
            } else if (target == SalesOrderStatus.CANCELLED) {
                sendSalesOrderCancelledNotification(salesOrderToEdit, "Sales order cancelled");
            }
        }

        // Update other fields
        if (request.getCustomerId() != null) salesOrderToEdit.setCustomerId(request.getCustomerId());
        if (request.getOrderDate() != null) salesOrderToEdit.setOrderDate(request.getOrderDate());
        if (request.getExpectedDeliveryDate() != null) salesOrderToEdit.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        if (request.getNotes() != null) salesOrderToEdit.setNotes(request.getNotes());
        if (request.getUpdatedByUserId() != null) salesOrderToEdit.setUpdatedByUserId(request.getUpdatedByUserId());
        if (request.getPaymentTerm() != null) salesOrderToEdit.setPaymentTerm(request.getPaymentTerm());

        // Handle line updates (you'll need to add reservation logic here too for line changes)
        if (request.getLines() != null) {
            List<SalesOrderLine> updatedLines = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (EditSalesOrderRequest.SalesOrderLineUpdateRequest lineUpdateRequest : request.getLines()) {
                Product product = productRepository.findByIdAndEntityStatusNot(lineUpdateRequest.getProductId(),
                        EntityStatus.DELETED).orElse(null);

                if (product == null) {
                    continue;
                }

                SalesOrderLine salesOrderLine;
                if (lineUpdateRequest.getSalesOrderLineId() != null) {
                    Optional<SalesOrderLine> existingLineOpt = salesOrderToEdit.getSalesOrderLines().stream()
                            .filter(line -> line.getId().equals(lineUpdateRequest.getSalesOrderLineId()))
                            .findFirst();

                    if (existingLineOpt.isPresent()) {
                        salesOrderLine = existingLineOpt.get();
                        modelMapper.map(lineUpdateRequest, salesOrderLine);
                        salesOrderLine.setUpdatedByUserId(request.getUpdatedByUserId());
                    } else {
                        salesOrderLine = modelMapper.map(lineUpdateRequest, SalesOrderLine.class);
                        salesOrderLine.setSalesOrder(salesOrderToEdit);
                        salesOrderLine.setCreatedByUserId(request.getUpdatedByUserId());
                    }
                } else {
                    salesOrderLine = modelMapper.map(lineUpdateRequest, SalesOrderLine.class);
                    salesOrderLine.setSalesOrder(salesOrderToEdit);
                    salesOrderLine.setCreatedByUserId(request.getUpdatedByUserId());
                }

                salesOrderLine.setProduct(product);
                BigDecimal lineTotal = salesOrderLine.getQuantity().multiply(salesOrderLine.getUnitPrice());
                salesOrderLine.setTotalPrice(lineTotal);
                totalAmount = totalAmount.add(lineTotal);

                updatedLines.add(salesOrderLine);
            }

            salesOrderToEdit.setSalesOrderLines(updatedLines);
            salesOrderToEdit.setTotalAmount(totalAmount);
        }

        if (request.getEntityStatus() != null) {
            salesOrderToEdit.setEntityStatus(request.getEntityStatus());
        }

        SalesOrder saved = salesOrderServiceAuditable.update(salesOrderToEdit, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesOrderDto salesOrderDto = modelMapper.map(saved, SalesOrderDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, salesOrderDto, null, null);
    }

    @Override
    @Transactional
    public SalesOrderResponse fulfillOrder(FulfillSalesOrderRequest request, String username, Locale locale) {

        String message;

        ValidatorDto validatorDto = validator.isFulfillOrderRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_FULFILL_SALES_ORDER_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        // Idempotency check: prevent duplicate processing on retries
        String idempotencyKey = request.getIdempotencyKey();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            boolean acquired = idempotencyService.tryAcquire(idempotencyKey, IdempotencyOperation.FULFILL_ORDER,
                    ReferenceDocumentType.SALES_ORDER.name());

            if (!acquired) {

                // Duplicate request detected. Attempt to return the original result using the idempotency linkage
                try {

                    Optional<IdempotencyKey> idempotencyKeyOptional = idempotencyService.findByKey(idempotencyKey);

                    if (idempotencyKeyOptional.isPresent() && idempotencyKeyOptional.get().getReferenceId() != null) {

                        Long soId = idempotencyKeyOptional.get().getReferenceId();
                        Optional<SalesOrder> salesOrder = salesOrderRepository.findByIdAndEntityStatusNot(soId,
                                EntityStatus.DELETED);

                        if (salesOrder.isPresent()) {

                            SalesOrderDto existingDto = modelMapper.map(salesOrder.get(), SalesOrderDto.class);

                            String successMsg = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_FULFILLED_SUCCESSFULLY.getCode(),
                                    new String[]{}, locale);

                            return buildResponse(200, true, successMsg, existingDto, null,
                                    null);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Idempotency duplicate-path lookup failed for key {}: {}", idempotencyKey, ex.getMessage());
                }

                // Fallback: return a generic success (operation already processed previously)
                message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_FULFILLED_SUCCESSFULLY.getCode(),
                        new String[]{}, locale);
                return buildResponse(200, true, message, null, null, null);
            }
        }

        Optional<SalesOrder> existingSalesOrder = findSalesOrderForUser(
                request.getSalesOrderId(), username, locale);

        if (existingSalesOrder.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrder salesOrder = existingSalesOrder.get();

        if (salesOrder.getStatus() != SalesOrderStatus.CONFIRMED &&
                salesOrder.getStatus() != SalesOrderStatus.APPROVED &&
                salesOrder.getStatus() != SalesOrderStatus.PARTIALLY_SHIPPED) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FULFILLABLE.getCode(), new String[]{},
                    locale);

            return buildResponse(400, false, message, null, null, null);
        }

        // PRE-VALIDATION: Validate all fulfillment items before processing any
        List<String> preValidationErrors = new ArrayList<>();

        for (FulfillSalesOrderRequest.FulfilledLineItem fulfilledItem : request.getFulfilledItems()) {

            Optional<SalesOrderLine> salesOrderLine = salesOrder.getSalesOrderLines().stream()
                    .filter(line -> line.getId().equals(fulfilledItem.getSalesOrderLineId()))
                    .findFirst();

            if (salesOrderLine.isEmpty()) {
                preValidationErrors.add("Sales order line ID " + fulfilledItem.getSalesOrderLineId() +
                        " not found on this order.");
                continue;
            }

            SalesOrderLine orderLine = salesOrderLine.get();

            // Validate fulfillment quantity doesn't exceed remaining quantity
            BigDecimal remainingQuantity = orderLine.getQuantity().subtract(
                    orderLine.getFulfilledQuantity() != null ? orderLine.getFulfilledQuantity() : BigDecimal.ZERO);

            if (fulfilledItem.getQuantityFulfilled().compareTo(remainingQuantity) > 0) {
                preValidationErrors.add("Line " + fulfilledItem.getSalesOrderLineId() +
                        ": Cannot fulfill " + fulfilledItem.getQuantityFulfilled() +
                        ". Remaining quantity is " + remainingQuantity);
            }

            // Check reservation exists (validates inventory was properly allocated)
            Optional<InventoryItem> inventoryItemOpt = inventoryItemService
                    .findInventoryItemByProductIdAndWarehouseId(orderLine.getProduct().getId(),
                            request.getWarehouseLocationId());

            if (inventoryItemOpt.isEmpty()) {
                preValidationErrors.add("No inventory found for product " + orderLine.getProduct().getName() +
                        " at specified warehouse.");
                continue;
            }

            InventoryItem inventoryItem = inventoryItemOpt.get();
            BigDecimal reservedQuantity = inventoryItem.getReservedQuantity() != null ?
                    inventoryItem.getReservedQuantity() : BigDecimal.ZERO;

            if (reservedQuantity.compareTo(fulfilledItem.getQuantityFulfilled()) < 0) {
                preValidationErrors.add("Insufficient reserved inventory for product " + orderLine.getProduct().getName() +
                        ". Required: " + fulfilledItem.getQuantityFulfilled() + ", Reserved: " + reservedQuantity);
            }

            // Additionally ensure there are reservation rows for this order line at the target warehouse
            List<InventoryReservation> reservationsForLine =
                    inventoryReservationRepository
                            .findBySalesOrderIdAndSalesOrderLineIdAndWarehouseLocation_IdAndStatusOrderByCreatedAtAsc(
                                    salesOrder.getId(),
                                    fulfilledItem.getSalesOrderLineId(),
                                    request.getWarehouseLocationId(),
                                    ReservationStatus.ACTIVE);

            java.math.BigDecimal reservedForLine = reservationsForLine.stream()
                    .map(InventoryReservation::getQuantity)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            if (reservedForLine.compareTo(fulfilledItem.getQuantityFulfilled()) < 0) {
                preValidationErrors.add("Reserved rows for line " + fulfilledItem.getSalesOrderLineId() +
                        " at this warehouse do not cover the quantity to fulfill. Required: " +
                        fulfilledItem.getQuantityFulfilled() + ", Reserved Rows Sum: " + reservedForLine);
            }
        }

        if (!preValidationErrors.isEmpty()) {
            message = "Cannot fulfill order: " + String.join(", ", preValidationErrors);
            return buildResponseWithErrors(400, false, message, null, null, preValidationErrors);
        }

        // Process fulfillment - now safe because pre-validated
        List<SalesOrderLine> updatedLines = new ArrayList<>();

        try {

            for (FulfillSalesOrderRequest.FulfilledLineItem fulfilledItem : request.getFulfilledItems()) {

                Optional<SalesOrderLine> salesOrderLine = salesOrder.getSalesOrderLines().stream()
                        .filter(line -> line.getId().equals(fulfilledItem.getSalesOrderLineId()))
                        .findFirst();

                SalesOrderLine orderLine = salesOrderLine.get(); // Safe because pre-validated

                // Find inventory item
                Optional<InventoryItem> inventoryItemOpt = inventoryItemService
                        .findInventoryItemByProductIdAndWarehouseId(orderLine.getProduct().getId(),
                                request.getWarehouseLocationId());

                InventoryItem inventoryItem = inventoryItemOpt.get(); // Safe because pre-validated

                // Create stock out transaction (this fulfills from reserved inventory)
                inventoryItemService.createStockOut(
                        inventoryItem.getId(),
                        fulfilledItem.getQuantityFulfilled(),
                        "Sales order fulfillment: " + salesOrder.getSalesOrderNumber(),
                        request.getFulfilledByUserId(),
                        salesOrder.getId(),
                        ReferenceDocumentType.SALES_ORDER,
                        locale,
                        username
                );

                // Consume matching reservation rows (FIFO by createdAt) and adjust aggregate
                List<InventoryReservation> reservationsForLine =
                        inventoryReservationRepository
                                .findBySalesOrderIdAndSalesOrderLineIdAndWarehouseLocation_IdAndStatusOrderByCreatedAtAsc(
                                        salesOrder.getId(),
                                        fulfilledItem.getSalesOrderLineId(),
                                        request.getWarehouseLocationId(),
                                        ReservationStatus.ACTIVE);

                BigDecimal remainingToConsume = fulfilledItem.getQuantityFulfilled();
                for (InventoryReservation res : reservationsForLine) {
                    if (remainingToConsume.compareTo(BigDecimal.ZERO) <= 0) break;
                    BigDecimal consume = res.getQuantity().min(remainingToConsume);
                    BigDecimal newQty = res.getQuantity().subtract(consume);
                    if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                        // Fully consumed: mark as CONSUMED instead of deleting
                        res.setQuantity(BigDecimal.ZERO);
                        res.setStatus(ReservationStatus.CONSUMED);
                        inventoryReservationRepository.save(res);
                    } else {
                        // Partially consumed: reduce remaining ACTIVE quantity
                        res.setQuantity(newQty);
                        inventoryReservationRepository.save(res);
                    }
                    remainingToConsume = remainingToConsume.subtract(consume);
                }

                // Adjust aggregate reserved quantity on the inventory item
                BigDecimal currentReserved = inventoryItem.getReservedQuantity() != null ?
                        inventoryItem.getReservedQuantity() : BigDecimal.ZERO;
                if (fulfilledItem.getQuantityFulfilled().compareTo(currentReserved) > 0) {
                    log.warn("Consuming reservation exceeds current reserved: itemId={}, currentReserved={}, consumeQty={}, salesOrderId={}",
                            inventoryItem.getId(), currentReserved, fulfilledItem.getQuantityFulfilled(), salesOrder.getId());
                }
                BigDecimal newReserved = currentReserved.subtract(fulfilledItem.getQuantityFulfilled()).max(BigDecimal.ZERO);
                inventoryItem.setReservedQuantity(newReserved);

                // Update fulfilled quantity on the line
                BigDecimal currentFulfilled = orderLine.getFulfilledQuantity() != null ?
                        orderLine.getFulfilledQuantity() : BigDecimal.ZERO;

                BigDecimal newFulfilled = currentFulfilled.add(fulfilledItem.getQuantityFulfilled());
                orderLine.setFulfilledQuantity(newFulfilled);
                orderLine.setUpdatedByUserId(request.getFulfilledByUserId());

                updatedLines.add(orderLine);
            }

            // Determine if an order is fully fulfilled
            boolean allLinesFulfilled = salesOrder.getSalesOrderLines().stream()
                    .allMatch(line -> {
                        BigDecimal fulfilled = line.getFulfilledQuantity() != null ?
                                line.getFulfilledQuantity() : BigDecimal.ZERO;
                        return fulfilled.compareTo(line.getQuantity()) >= 0;
                    });

            // Update order status
            if (allLinesFulfilled) {
                salesOrder.setStatus(SalesOrderStatus.SHIPPED);
                message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_FULFILLED_SUCCESSFULLY.getCode(),
                        new String[]{}, locale);
            } else {
                salesOrder.setStatus(SalesOrderStatus.PARTIALLY_SHIPPED);
                message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_PARTIALLY_FULFILLED.getCode(),
                        new String[]{}, locale);
            }

            salesOrder.setUpdatedByUserId(request.getFulfilledByUserId());
            SalesOrder updatedSalesOrder = salesOrderServiceAuditable.update(salesOrder, locale, username);

            // Send appropriate notifications based on fulfillment status
            sendSalesOrderShippedNotification(updatedSalesOrder);

            SalesOrderDto salesOrderDto = modelMapper.map(updatedSalesOrder, SalesOrderDto.class);

            // Mark idempotency as processed if key was provided
            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                idempotencyService.markProcessed(request.getIdempotencyKey(), updatedSalesOrder.getId());
            }

            return buildResponse(200, true, message, salesOrderDto, null, null);

        } catch (Exception e) {
            // Transaction will auto-rollback due to @Transactional
            log.error("Failed to fulfill sales order {}: {}", salesOrder.getId(), e.getMessage(), e);
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_FULFILLMENT_FAILED.getCode(),
                    new String[]{e.getMessage()}, locale);
            throw new RuntimeException(message, e); // Force rollback
        }
    }

    @Override
    @Transactional
    public SalesOrderResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<SalesOrder> existingOpt = findSalesOrderForUser(id, username, locale);

        if (existingOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrder salesOrderToDelete = existingOpt.get();

        // Check if order can be deleted
        if (salesOrderToDelete.getStatus() == SalesOrderStatus.FULFILLED ||
                salesOrderToDelete.getStatus() == SalesOrderStatus.PARTIALLY_SHIPPED) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_CANNOT_DELETE_FULFILLED.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        salesOrderToDelete.setEntityStatus(EntityStatus.DELETED);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesOrder saved = salesOrderServiceAuditable.delete(salesOrderToDelete, locale);

        // Send cancellation notification
        sendSalesOrderCancelledNotification(saved, "Sales order deleted");

        SalesOrderDto salesOrderDto = modelMapper.map(saved, SalesOrderDto.class);
        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_DELETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, salesOrderDto, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponse getFinancialSummary(Long salesOrderId, Long organizationId, Locale locale, String username) {

        String message = "";

        ValidatorDto salesOrderIdValidator = validator.isIdValid(salesOrderId, locale);

        if (!salesOrderIdValidator.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    salesOrderIdValidator.getErrorMessages());
        }

        ValidatorDto organizationIdValidator = validator.isIdValid(organizationId, locale);

        if (!organizationIdValidator.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    organizationIdValidator.getErrorMessages());
        }

        Optional<SalesOrder> salesOrderOpt = salesOrderRepository
                .findByIdAndSupplierOrganizationIdAndEntityStatusNot(
                        salesOrderId, organizationId, EntityStatus.DELETED);

        if (salesOrderOpt.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        SalesOrder salesOrder = salesOrderOpt.get();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        SalesOrderDto salesOrderDto = modelMapper.map(salesOrder, SalesOrderDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, salesOrderDto, null, null);
    }

    @Override
    public SalesOrderResponse findByMultipleFilters(SalesOrderMultipleFiltersRequest request, String username,
                                                    Locale locale) {

        String message = "";

        Specification<SalesOrder> spec = SalesOrderSpecification.deleted();

        if (!isSystemUser(username)) {
            Long organizationId = resolveOrganizationId(username, locale);
            if (organizationId == null) {
                message = "Organization could not be resolved for user";
                return buildResponse(400, false, message, null, null, null);
            }
            spec = spec.and(SalesOrderSpecification.supplierOrganizationIdEquals(organizationId));
        }

        if (request == null || request.getPage() < 0 || request.getSize() <= 0) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (request.getSalesOrderNumber() != null && !request.getSalesOrderNumber().isBlank()) {
            spec = spec.and(SalesOrderSpecification.salesOrderNumberLike(request.getSalesOrderNumber()));
        }

        if (request.getCustomerId() != null) {
            spec = spec.and(SalesOrderSpecification.customerIdEquals(request.getCustomerId()));
        }

        if (request.getStatus() != null) {
            spec = spec.and(SalesOrderSpecification.statusEquals(request.getStatus()));
        }

        if (request.getOrderDate() != null) {
            spec = spec.and(SalesOrderSpecification.orderDateBetween(request.getOrderDate(), request.getOrderDate()));
        }

        if (request.getExpectedDeliveryDate() != null) {
            spec = spec.and(SalesOrderSpecification.expectedDeliveryDateAfter(request.getExpectedDeliveryDate())
                    .and(SalesOrderSpecification.expectedDeliveryDateBefore(request.getExpectedDeliveryDate().
                            plusDays(1))));
        }

        if (request.getEntityStatus() != null) {
            spec = spec.and(SalesOrderSpecification.entityStatusEquals(request.getEntityStatus()));
        }

        if (request.getSearchValue() != null && !request.getSearchValue().isBlank()) {
            spec = spec.and(SalesOrderSpecification.any(request.getSearchValue()));
        }

        long totalCount = salesOrderRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage && totalCount > 0) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Page<SalesOrder> result = salesOrderRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Page<SalesOrderDto> salesOrderDtoPage = result.map(order -> modelMapper.map(order, SalesOrderDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        SalesOrderResponse response = buildResponse(200, true, message, null, null, null);
        response.setSalesOrderDtoPage(salesOrderDtoPage);
        return response;
    }

    @Override
    @Transactional
    public void allocateInventoryForOrder(SalesOrder salesOrder, Locale locale, String username) {
        // Use InventoryAllocationService instead of separate logic
        Long warehouseId = getDefaultWarehouseId(salesOrder.getCustomerId());

        InventoryAllocationServiceImpl.AllocationResult result =
                inventoryAllocationService.allocateInventory(salesOrder, warehouseId);

        if (!result.isSuccess()) {
            throw new IllegalStateException("Cannot allocate inventory: " +
                    String.join(", ", result.getErrors()));
        }
    }

    @Override
    @Transactional
    public void releaseInventoryReservations(SalesOrder salesOrder, Locale locale, String username) {
        Long warehouseId = getDefaultWarehouseId(salesOrder.getCustomerId());
        inventoryAllocationService.releaseAllocation(salesOrder, warehouseId);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<SalesOrderDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (SalesOrderDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(safe(item.getSalesOrderNumber())).append(",")
                    .append(item.getCustomerId() != null ? item.getCustomerId() : "").append(",")
                    .append(item.getStatus() != null ? item.getStatus().name() : "").append(",")
                    .append(item.getOrderDate() != null ? item.getOrderDate() : "").append(",")
                    .append(item.getExpectedDeliveryDate() != null ? item.getExpectedDeliveryDate() : "").append(",")
                    .append(item.getDeliveredDate() != null ? item.getDeliveredDate() : "").append(",")
                    .append(item.getTotalAmount() != null ? item.getTotalAmount() : "").append(",")
                    .append(safe(item.getNotes()))
                    .append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<SalesOrderDto> items) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales Orders");
        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;
        for (SalesOrderDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(safe(item.getSalesOrderNumber()));
            row.createCell(2).setCellValue(item.getCustomerId() != null ? item.getCustomerId() : 0);
            row.createCell(3).setCellValue(item.getStatus() != null ? item.getStatus().name() : "");
            row.createCell(4).setCellValue(item.getOrderDate() != null ? item.getOrderDate().toString() : "");
            row.createCell(5).setCellValue(item.getExpectedDeliveryDate() != null ? item.getExpectedDeliveryDate().toString() : "");
            row.createCell(6).setCellValue(item.getDeliveredDate() != null ? item.getDeliveredDate().toString() : "");
            row.createCell(7).setCellValue(item.getTotalAmount() != null ? item.getTotalAmount().doubleValue() : 0);
            row.createCell(8).setCellValue(safe(item.getNotes()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<SalesOrderDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (SalesOrderDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    safe(item.getSalesOrderNumber()),
                    String.valueOf(item.getCustomerId() != null ? item.getCustomerId() : 0),
                    item.getStatus() != null ? item.getStatus().name() : "",
                    item.getOrderDate() != null ? item.getOrderDate().toString() : "",
                    item.getExpectedDeliveryDate() != null ? item.getExpectedDeliveryDate().toString() : "",
                    item.getDeliveredDate() != null ? item.getDeliveredDate().toString() : "",
                    item.getTotalAmount() != null ? item.getTotalAmount().toString() : "0",
                    safe(item.getNotes())
            });
        }
        return InventoryExportSupport.writeTabularPdf("Sales Orders", "INV-SOD",
                "Sales order export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importSalesOrderFromCsv(InputStream csvInputStream) throws IOException {
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
                    CreateSalesOrderRequest request = new CreateSalesOrderRequest();
                    String customerIdStr = record.isMapped("CUSTOMER_ID") ? record.get("CUSTOMER_ID") : null;

                    if (customerIdStr != null && !customerIdStr.isBlank()) {
                        request.setCustomerId(Long.parseLong(customerIdStr.trim()));
                    }

                    String orderDateStr = record.isMapped("ORDER_DATE") ? record.get("ORDER_DATE") : null;
                    if (orderDateStr != null && !orderDateStr.isBlank()) {
                        request.setOrderDate(LocalDate.parse(orderDateStr.trim()));
                    }

                    String expectedDateStr = record.isMapped("EXPECTED_DELIVERY_DATE") ? record.get("EXPECTED_DELIVERY_DATE") : null;
                    if (expectedDateStr != null && !expectedDateStr.isBlank()) {
                        request.setExpectedDeliveryDate(LocalDate.parse(expectedDateStr.trim()));
                    }

                    String notes = record.isMapped("NOTES") ? record.get("NOTES") : null;
                    request.setNotes(notes);

                    SalesOrderResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
                ? "Import completed successfully. " + success + " out of " + total + " sales orders imported."
                : "Import failed. No sales orders were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private String generateSalesOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String millis = String.valueOf(System.currentTimeMillis());
        String suffix = millis.substring(millis.length() - 4);
        return "SO-" + datePart + "-" + suffix;
    }

    private SalesOrderResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                             SalesOrderDto dto, List<SalesOrderDto> dtoList,
                                             List<String> errorMessages) {
        SalesOrderResponse response = new SalesOrderResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setSalesOrderDto(dto);
        response.setSalesOrderDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private SalesOrderResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                       SalesOrderDto dto, List<SalesOrderDto> dtoList,
                                                       List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }

    private boolean isValidSalesOrderStatusTransition(SalesOrderStatus currentStatus, SalesOrderStatus newStatus) {

        if (currentStatus == newStatus) {
            return true;
        }

        return switch (currentStatus) {
            // Payment-gated procurement flow
            case PENDING_APPROVAL -> newStatus == SalesOrderStatus.APPROVED
                    || newStatus == SalesOrderStatus.CANCELLED;
            case APPROVED -> newStatus == SalesOrderStatus.PARTIALLY_SHIPPED
                    || newStatus == SalesOrderStatus.SHIPPED
                    || newStatus == SalesOrderStatus.CANCELLED;
            // AWAITING_RECEIPT: Can only transition to PENDING (when goods received) or CANCELLED
            case AWAITING_RECEIPT -> newStatus == SalesOrderStatus.PENDING
                    || newStatus == SalesOrderStatus.CANCELLED;
            case PENDING -> newStatus == SalesOrderStatus.CONFIRMED || newStatus == SalesOrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == SalesOrderStatus.PARTIALLY_SHIPPED
                    || newStatus == SalesOrderStatus.SHIPPED
                    || newStatus == SalesOrderStatus.CANCELLED;
            case PARTIALLY_SHIPPED -> newStatus == SalesOrderStatus.SHIPPED;
            case SHIPPED -> newStatus == SalesOrderStatus.DELIVERED;
            case DELIVERED -> newStatus == SalesOrderStatus.FULFILLED;
            case FULFILLED, CANCELLED -> false; // Terminal states
        };
    }

    private Long getDefaultWarehouseId(Long customerId) {

        try {

            List<WarehouseLocation> availableWarehouses = warehouseLocationRepository
                    .findByEntityStatusNotOrderByIdAsc(EntityStatus.DELETED);

            if (availableWarehouses.isEmpty()) {
                throw new IllegalStateException("No active warehouses found");
            }

            // Find a warehouse with the best overall inventory availability
            Long bestStockedWarehouseId = findBestStockedWarehouse(availableWarehouses);

            if (bestStockedWarehouseId != null) {
                return bestStockedWarehouseId;
            } else {
                return availableWarehouses.get(0).getId();
            }

        } catch (Exception e) {
            log.warn("Error determining default warehouse for customer {}: {}. Using fallback.",
                    customerId, e.getMessage());

            // Fallback: Return the first active warehouse or default warehouse ID
            return warehouseLocationRepository.findFirstByEntityStatusNotOrderByIdAsc(EntityStatus.DELETED)
                    .map(WarehouseLocation::getId)
                    .orElse(1L); // Ultimate fallback
        }
    }

    private Long findBestStockedWarehouse(List<WarehouseLocation> warehouses) {

        try {

            Map<Long, BigDecimal> warehouseStockLevels = new HashMap<>();

            // Calculate total available inventory per warehouse
            for (WarehouseLocation warehouse : warehouses) {

                BigDecimal totalAvailableStock = inventoryItemRepository
                        .findByWarehouseLocationIdAndEntityStatusNot(warehouse.getId(), EntityStatus.DELETED)
                        .stream()
                        .map(InventoryItem::getAvailableQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                warehouseStockLevels.put(warehouse.getId(), totalAvailableStock);
            }

            // Return warehouse with the highest total available stock
            return warehouseStockLevels.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

        } catch (Exception e) {
            log.debug("Could not determine best stocked warehouse: {}", e.getMessage());
            return null;
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
     * Helper method to fetch customer details
     */
    private OrganizationResponse getCustomerDetails(Long customerId) {
        OrganizationResponse customerResponse = new OrganizationResponse();
        
        try {
            // Fetch customer details
            UserResponse userResponse = userManagementServiceClient.findById(customerId, Locale.ENGLISH);
            
            customerResponse = organizationServiceClient.findById(userResponse.getUserDto().getOrganizationId(), Locale.ENGLISH);
            
        } catch (Exception e) {
            log.error("Failed to fetch customer for customer ID: {}. Error: {}", customerId, e.getMessage());
        }
        
        return customerResponse;
    }

    private boolean isSystemUser(String username) {
        return username != null && "SYSTEM".equalsIgnoreCase(username);
    }

    private Long resolveOrganizationId(String username, Locale locale) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String principal = username.trim();
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(principal);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getOrganizationId() != null
                    && userResponse.getUserDto().getOrganizationId() > 0) {
                return userResponse.getUserDto().getOrganizationId();
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve organization via session profile for user {}: {}", principal, ex.getMessage());
        }

        try {
            UserResponse userResponse = userManagementServiceClient.findByPhoneNumberOrEmail(principal, locale);
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getOrganizationId() != null
                    && userResponse.getUserDto().getOrganizationId() > 0) {
                return userResponse.getUserDto().getOrganizationId();
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve organization for user {}: {}", principal, ex.getMessage());
        }

        return null;
    }

    private Optional<SalesOrder> findSalesOrderForUser(Long salesOrderId, String username, Locale locale) {
        if (salesOrderId == null || salesOrderId <= 0) {
            return Optional.empty();
        }

        if (isSystemUser(username)) {
            return salesOrderRepository.findByIdAndEntityStatusNot(salesOrderId, EntityStatus.DELETED);
        }

        Long organizationId = resolveOrganizationId(username, locale);
        if (organizationId == null) {
            return Optional.empty();
        }

        return salesOrderRepository.findByIdAndSupplierOrganizationIdAndEntityStatusNot(
                salesOrderId, organizationId, EntityStatus.DELETED);
    }

    /**
     * Send a notification when a new sales order is created
     */
    private void sendSalesOrderCreatedNotification(SalesOrder salesOrder) {

        try {
            // Send it to the customer
            sendSalesOrderCreatedToCustomer(salesOrder);
            
            // Send it to internal sales team
            sendSalesOrderCreatedToInternal(salesOrder);
            
            log.info("Successfully sent sales order created notifications for SO: {}", 
                    salesOrder.getSalesOrderNumber());
                    
        } catch (Exception e) {
            log.error("Failed to send sales order created notification for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send sales order created notification specifically to the customer
     */
    private void sendSalesOrderCreatedToCustomer(SalesOrder salesOrder) {
        try {
            Map<String, Object> customerData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "orderDate", salesOrder.getOrderDate() != null ? salesOrder.getOrderDate().toString() : "",
                    "expectedDeliveryDate", salesOrder.getExpectedDeliveryDate() != null ? salesOrder.getExpectedDeliveryDate().toString() : "",
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "totalLines", salesOrder.getSalesOrderLines() != null ? salesOrder.getSalesOrderLines().size() : 0,
                    "notes", salesOrder.getNotes() != null ? salesOrder.getNotes() : ""
            );

            // Get customer contact information
            OrganizationResponse customerResponse = getCustomerDetails(salesOrder.getCustomerId());

            if (customerResponse != null && customerResponse.getOrganizationDto() != null) {
                
                // Send email to customer
                if (customerResponse.getOrganizationDto().getEmail() != null && 
                        !customerResponse.getOrganizationDto().getEmail().isBlank()) {
                    
                    NotificationRequest.Recipient customerEmailRecipient = new NotificationRequest.Recipient(
                            null, // External recipient
                            customerResponse.getOrganizationDto().getEmail(),
                            null,
                            null
                    );
                    
                    NotificationRequest customerEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "SALES_ORDER_CREATED_CUSTOMER_EMAIL", // Customer-specific template
                            customerEmailRecipient,
                            customerData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", customerEmailNotification);
                }
                
                // Send SMS to customer
                if (customerResponse.getOrganizationDto().getPhoneNumber() != null && 
                        !customerResponse.getOrganizationDto().getPhoneNumber().isBlank()) {
                    
                    NotificationRequest.Recipient customerPhoneRecipient = new NotificationRequest.Recipient(
                            null, // External recipient
                            null,
                            customerResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );
                    
                    NotificationRequest customerPhoneNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "SALES_ORDER_CREATED_CUSTOMER_SMS", // Customer-specific SMS template
                            customerPhoneRecipient,
                            customerData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", customerPhoneNotification);
                }
            }
            
            log.info("Sent sales order created notification to customer for SO: {}", 
                    salesOrder.getSalesOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send sales order created notification to customer for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send sales order created notification to internal sales team
     */
    private void sendSalesOrderCreatedToInternal(SalesOrder salesOrder) {

        try {
            Map<String, Object> internalData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "customerId", salesOrder.getCustomerId(),
                    "orderDate", salesOrder.getOrderDate() != null ? salesOrder.getOrderDate().toString() : "",
                    "expectedDeliveryDate", salesOrder.getExpectedDeliveryDate() != null ? salesOrder.getExpectedDeliveryDate().toString() : "",
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "totalLines", salesOrder.getSalesOrderLines() != null ? salesOrder.getSalesOrderLines().size() : 0,
                    "status", salesOrder.getStatus().toString(),
                    "notes", salesOrder.getNotes() != null ? salesOrder.getNotes() : ""
            );

            NotificationRequest.Recipient internalRecipient = new NotificationRequest.Recipient(
                    null, // Internal recipient - will be handled by notification service
                    null,
                    null,
                    null
            );
            
            NotificationRequest internalNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "SALES_ORDER_CREATED_INTERNAL_EMAIL", // Internal template
                    internalRecipient,
                    internalData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", internalNotification);
            
            log.info("Sent sales order created notification to internal team for SO: {}", 
                    salesOrder.getSalesOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send sales order created notification to internal team for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send a notification when a sales order is confirmed
     */
    private void sendSalesOrderConfirmedNotification(SalesOrder salesOrder) {

        try {
            sendSalesOrderConfirmedToCustomer(salesOrder);
            sendSalesOrderConfirmedToInternal(salesOrder);
            
            log.info("Successfully sent sales order confirmed notifications for SO: {}", 
                    salesOrder.getSalesOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send sales order confirmed notification for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    private void sendSalesOrderConfirmedToCustomer(SalesOrder salesOrder) {

        try {
            Map<String, Object> customerData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "orderDate", salesOrder.getOrderDate() != null ? salesOrder.getOrderDate().toString() : "",
                    "expectedDeliveryDate", salesOrder.getExpectedDeliveryDate() != null ? salesOrder.getExpectedDeliveryDate().toString() : "",
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "status", "CONFIRMED"
            );

            OrganizationResponse customerResponse = getCustomerDetails(salesOrder.getCustomerId());

            if (customerResponse != null && customerResponse.getOrganizationDto() != null) {
                
                if (customerResponse.getOrganizationDto().getEmail() != null && 
                        !customerResponse.getOrganizationDto().getEmail().isBlank()) {
                    
                    NotificationRequest customerEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "SALES_ORDER_CONFIRMED_CUSTOMER_EMAIL",
                            new NotificationRequest.Recipient(null, customerResponse.getOrganizationDto().getEmail(),
                                    null, null),
                            customerData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                            customerEmailNotification);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send sales order confirmed notification to customer for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    private void sendSalesOrderConfirmedToInternal(SalesOrder salesOrder) {

        try {
            Map<String, Object> internalData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "customerId", salesOrder.getCustomerId(),
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "status", "CONFIRMED"
            );

            NotificationRequest internalNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "SALES_ORDER_CONFIRMED_WAREHOUSE_EMAIL",
                    new NotificationRequest.Recipient(null, null, null, null),
                    internalData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                    internalNotification);
        } catch (Exception e) {
            log.error("Failed to send sales order confirmed notification to internal team for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send a notification when a sales order is shipped
     */
    private void sendSalesOrderShippedNotification(SalesOrder salesOrder) {

        try {
            sendSalesOrderShippedToCustomer(salesOrder);
            
            log.info("Successfully sent sales order shipped notification for SO: {}", 
                    salesOrder.getSalesOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send sales order shipped notification for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    private void sendSalesOrderShippedToCustomer(SalesOrder salesOrder) {
        try {
            Map<String, Object> customerData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "orderDate", salesOrder.getOrderDate() != null ? salesOrder.getOrderDate().toString() : "",
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "status", "SHIPPED",
                    "trackingInfo", "Your order has been shipped and is on its way!"
            );

           OrganizationResponse customerResponse = getCustomerDetails(salesOrder.getCustomerId());

            if (customerResponse != null && customerResponse.getOrganizationDto() != null) {
                
                // Send email with tracking info
                if (customerResponse.getOrganizationDto().getEmail() != null && 
                        !customerResponse.getOrganizationDto().getEmail().isBlank()) {
                    
                    NotificationRequest customerEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "SALES_ORDER_SHIPPED_CUSTOMER_EMAIL",
                            new NotificationRequest.Recipient(null, customerResponse.getOrganizationDto().getEmail(),
                                    null, null),
                            customerData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                            customerEmailNotification);
                }
                
                // Send SMS with tracking info
                if (customerResponse.getOrganizationDto().getPhoneNumber() != null && 
                        !customerResponse.getOrganizationDto().getPhoneNumber().isBlank()) {
                    
                    NotificationRequest customerSmsNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "SALES_ORDER_SHIPPED_CUSTOMER_SMS",
                            new NotificationRequest.Recipient(null, null,
                                    customerResponse.getOrganizationDto().getPhoneNumber(), null),
                            customerData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                            customerSmsNotification);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send sales order shipped notification to customer for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send a notification when a sales order is delivered
     */
    private void sendSalesOrderDeliveredNotification(SalesOrder salesOrder) {
        try {
            sendSalesOrderDeliveredToCustomer(salesOrder);
            sendSalesOrderDeliveredToInternal(salesOrder);
            
            log.info("Successfully sent sales order delivered notification for SO: {}", 
                    salesOrder.getSalesOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send sales order delivered notification for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    private void sendSalesOrderDeliveredToCustomer(SalesOrder salesOrder) {
        try {
            Map<String, Object> customerData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "orderDate", salesOrder.getOrderDate() != null ? salesOrder.getOrderDate().toString() : "",
                    "deliveredDate", salesOrder.getDeliveredDate() != null ? salesOrder.getDeliveredDate().toString() : "",
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "status", "DELIVERED"
            );

            OrganizationResponse customerResponse = getCustomerDetails(salesOrder.getCustomerId());

            if (customerResponse != null && customerResponse.getOrganizationDto() != null) {
                
                if (customerResponse.getOrganizationDto().getEmail() != null && 
                        !customerResponse.getOrganizationDto().getEmail().isBlank()) {
                    
                    NotificationRequest customerEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "SALES_ORDER_DELIVERED_CUSTOMER_EMAIL",
                            new NotificationRequest.Recipient(null, customerResponse.getOrganizationDto().getEmail(),
                                    null, null),
                            customerData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                            customerEmailNotification);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send sales order delivered notification to customer for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    private void sendSalesOrderDeliveredToInternal(SalesOrder salesOrder) {

        try {
            Map<String, Object> internalData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "customerId", salesOrder.getCustomerId(),
                    "deliveredDate", salesOrder.getDeliveredDate() != null ? salesOrder.getDeliveredDate().toString() : "",
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "status", "DELIVERED"
            );

            NotificationRequest internalNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "SALES_ORDER_DELIVERED_INTERNAL_EMAIL",
                    new NotificationRequest.Recipient(null, null, null, null),
                    internalData,
                    null
            );
            
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                    internalNotification);
        } catch (Exception e) {
            log.error("Failed to send sales order delivered notification to internal team for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send a notification when a sales order is cancelled
     */
    private void sendSalesOrderCancelledNotification(SalesOrder salesOrder, String reason) {

        try {
            sendSalesOrderCancelledToCustomer(salesOrder, reason);
            
            log.info("Successfully sent sales order cancelled notification for SO: {}", 
                    salesOrder.getSalesOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send sales order cancelled notification for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }

    private void sendSalesOrderCancelledToCustomer(SalesOrder salesOrder, String reason) {

        try {
            Map<String, Object> customerData = Map.of(
                    "salesOrderNumber", salesOrder.getSalesOrderNumber(),
                    "orderDate", salesOrder.getOrderDate() != null ? salesOrder.getOrderDate().toString() : "",
                    "totalAmount", salesOrder.getTotalAmount() != null ? salesOrder.getTotalAmount().toString() : "0.00",
                    "status", "CANCELLED",
                    "reason", reason != null ? reason : "Sales order was cancelled"
            );

            OrganizationResponse customerResponse = getCustomerDetails(salesOrder.getCustomerId());

            if (customerResponse != null && customerResponse.getOrganizationDto() != null) {
                
                if (customerResponse.getOrganizationDto().getEmail() != null && 
                        !customerResponse.getOrganizationDto().getEmail().isBlank()) {
                    
                    NotificationRequest customerEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "SALES_ORDER_CANCELLED_CUSTOMER_EMAIL",
                            new NotificationRequest.Recipient(null, customerResponse.getOrganizationDto().getEmail(),
                                    null, null),
                            customerData,
                            null
                    );
                    
                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                            customerEmailNotification);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send sales order cancelled notification to customer for SO: {}. Error: {}", 
                    salesOrder.getSalesOrderNumber(), e.getMessage());
        }
    }
}
