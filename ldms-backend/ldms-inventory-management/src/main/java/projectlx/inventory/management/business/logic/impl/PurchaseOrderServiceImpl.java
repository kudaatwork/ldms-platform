package projectlx.inventory.management.business.logic.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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
import org.springframework.util.StringUtils;
import projectlx.inventory.management.business.auditable.api.GoodsReceivedVoucherServiceAuditable;
import projectlx.inventory.management.business.auditable.api.PurchaseOrderServiceAuditable;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.logic.api.GoodsReceiptProcessor;
import projectlx.inventory.management.business.logic.api.PurchaseOrderService;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.logic.api.PurchaseOrderLineService;
import projectlx.inventory.management.business.logic.api.IdempotencyService;
import projectlx.inventory.management.business.logic.support.OrganizationFunctionalCurrencySupport;
import projectlx.inventory.management.business.logic.support.TransactionCurrencyConversionSupport;
import projectlx.inventory.management.clients.dto.BillingConversionResultDto;
import projectlx.inventory.management.business.validator.api.PurchaseOrderServiceValidator;
import projectlx.inventory.management.clients.OrganizationServiceClient;
import projectlx.inventory.management.clients.UserManagementServiceClient;
import projectlx.inventory.management.events.PurchaseOrderApprovedEvent;
import projectlx.inventory.management.model.*;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.repository.GoodsReceivedVoucherRepository;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.PurchaseOrderSpecification;
import projectlx.inventory.management.utils.dtos.GoodsReceiptResult;
import projectlx.inventory.management.utils.dtos.PurchaseOrderDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.ReceiptValidationError;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.NotificationRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.inventory.management.utils.requests.CreateOrUpdateStockRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderServiceAuditable purchaseOrderServiceAuditable;
    private final ModelMapper modelMapper;
    private final PurchaseOrderServiceValidator validator;
    private final MessageService messageService;
    private final PurchaseOrderLineService purchaseOrderLineService;
    private final InventoryItemService inventoryItemService;
    private final GoodsReceivedVoucherRepository grvRepository;
    private final GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable;
    private final StockTransactionHistoryRepository stockTransactionHistoryRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OrganizationServiceClient organizationServiceClient;
    private final UserManagementServiceClient userManagementServiceClient;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;
    private final GoodsReceiptProcessor goodsReceiptProcessor;
    private final OrganizationFunctionalCurrencySupport organizationFunctionalCurrencySupport;
    private final TransactionCurrencyConversionSupport transactionCurrencyConversionSupport;

    private static final String[] HEADERS = {"ID", "PURCHASE_ORDER_NUMBER", "SUPPLIER_ID", "STATUS", "ORDER_DATE",
            "EXPECTED_DATE", "RECEIVED_DATE", "EXTERNAL_ID", "NOTES"};
    private static final String[] CSV_HEADERS = {"SUPPLIER_ID", "EXTERNAL_ID", "ORDER_DATE", "EXPECTED_DATE", "NOTES"};

    @Override
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isCreatePurchaseOrderRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplierId(request.getSupplierId());
        purchaseOrder.setStatus(request.getStatus() != null ? request.getStatus() : PurchaseOrderStatus.DRAFT);
        if (request.getOrderDate() != null) purchaseOrder.setOrderDate(request.getOrderDate());
        if (request.getExpectedDate() != null) purchaseOrder.setExpectedDate(request.getExpectedDate());
        purchaseOrder.setExternalId(request.getExternalId());
        purchaseOrder.setNotes(request.getNotes());
        purchaseOrder.setCreatedByUserId(request.getCreatedByUserId());
        purchaseOrder.setPurchaseOrderNumber(generatePurchaseOrderNumber());

        // NEW FIELDS - Party Information
        if (request.getOrganizationId() != null) {
            purchaseOrder.setOrganizationId(request.getOrganizationId());
        }
        if (request.getBuyerContact() != null) {
            purchaseOrder.setBuyerContact(request.getBuyerContact());
        }
        if (request.getSupplierContact() != null) {
            purchaseOrder.setSupplierContact(request.getSupplierContact());
        }

        // NEW FIELDS - Financial Terms
        if (StringUtils.hasText(request.getCurrency())) {
            purchaseOrder.setCurrency(request.getCurrency());
        } else if (request.getOrganizationId() != null) {
            purchaseOrder.setCurrency(organizationFunctionalCurrencySupport.resolveFunctionalCurrency(request.getOrganizationId()));
        }
        if (request.getPaymentTerm() != null) {
            purchaseOrder.setPaymentTerm(request.getPaymentTerm());
        }
        if (request.getPaymentDueDate() != null) {
            purchaseOrder.setPaymentDueDate(request.getPaymentDueDate());
        }
        if (request.getTaxRate() != null) {
            purchaseOrder.setTaxRate(request.getTaxRate());
        }
        if (request.getEarlyPaymentDiscountPct() != null) {
            purchaseOrder.setEarlyPaymentDiscountPct(request.getEarlyPaymentDiscountPct());
            purchaseOrder.setEarlyPaymentDiscountUntil(request.getEarlyPaymentDiscountUntil());
        }
        if (request.getPrepaymentRequired() != null && request.getPrepaymentRequired()) {
            purchaseOrder.setPrepaymentRequired(true);
            purchaseOrder.setPrepaymentPercent(request.getPrepaymentPercent());
        }

        // NEW FIELDS - Shipping & Logistics
        if (request.getShipFromLocationId() != null) {
            purchaseOrder.setShipFromLocationId(request.getShipFromLocationId());
        }
        if (request.getShipToLocationId() != null) {
            purchaseOrder.setShipToLocationId(request.getShipToLocationId());
        }
        if (request.getReceivingWarehouseId() != null) {
            purchaseOrder.setReceivingWarehouseId(request.getReceivingWarehouseId());
        }
        if (request.getFreightTerms() != null) {
            purchaseOrder.setFreightTerms(request.getFreightTerms());
        }
        if (request.getShipMode() != null) {
            purchaseOrder.setShipMode(request.getShipMode());
        }
        if (request.getShippingInstructions() != null) {
            purchaseOrder.setShippingInstructions(request.getShippingInstructions());
        }

        // NEW FIELDS - Import/Export
        if (request.getIsImport() != null && request.getIsImport()) {
            purchaseOrder.setIsImport(true);
            purchaseOrder.setCustomsDeclarationNumber(request.getCustomsDeclarationNumber());
            purchaseOrder.setPortOfEntry(request.getPortOfEntry());
        }

        List<PurchaseOrderLine> purchaseOrderLineList = new ArrayList<>();

        if (request.getLines() != null && !request.getLines().isEmpty()) {
            int lineNumber = 1;
            for (CreatePurchaseOrderRequest.PurchaseOrderLineRequest lineReq : request.getLines()) {

                Product product = productRepository.findByIdAndEntityStatusNot(lineReq.getProductId(),
                        EntityStatus.DELETED).orElse(null);

                if (product == null) {
                    continue;
                }

                PurchaseOrderLine purchaseOrderLine = new PurchaseOrderLine();
                purchaseOrderLine.setPurchaseOrder(purchaseOrder);
                purchaseOrderLine.setLineNumber(lineNumber++);
                purchaseOrderLine.setProduct(product);
                purchaseOrderLine.setUnitOfMeasure(lineReq.getUnitOfMeasure());
                purchaseOrderLine.setQuantity(lineReq.getQuantity());
                purchaseOrderLine.setUnitPrice(lineReq.getUnitPrice());
                purchaseOrderLine.setCreatedByUserId(request.getCreatedByUserId());
                purchaseOrderLineList.add(purchaseOrderLine);
            }
        }

        purchaseOrder.setPurchaseOrderLines(purchaseOrderLineList);

        // Calculate totals (subtotal, tax, total) before saving
        purchaseOrder.calculateTotals();
        applyFunctionalCurrencyConversion(purchaseOrder);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        PurchaseOrder saved = purchaseOrderServiceAuditable.create(purchaseOrder, locale, username);

        sendPurchaseOrderCreatedNotification(saved);

        PurchaseOrderDto purchaseOrderDto = modelMapper.map(saved, PurchaseOrderDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, message, purchaseOrderDto, null, null);
    }

    @Override
    public PurchaseOrderResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrder> purchaseOrder = purchaseOrderRepository.findById(id);

        if (purchaseOrder.isEmpty() || purchaseOrder.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        PurchaseOrderDto dto = modelMapper.map(purchaseOrder.get(), PurchaseOrderDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public PurchaseOrderResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<PurchaseOrder> purchaseOrderList = purchaseOrderRepository.findAll();

        purchaseOrderList = purchaseOrderList.stream().filter(purchaseOrder ->
                purchaseOrder.getEntityStatus() != EntityStatus.DELETED).toList();

        if (purchaseOrderList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<PurchaseOrderDto> purchaseOrderDtoList = purchaseOrderList.stream().map(purchaseOrder ->
                modelMapper.map(purchaseOrder, PurchaseOrderDto.class)).collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, null, purchaseOrderDtoList, null);
    }

    @Override
    public PurchaseOrderResponse update(EditPurchaseOrderRequest editPurchaseOrderRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = validator.isRequestValidForEditing(editPurchaseOrderRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PURCHASE_ORDER_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrder> existingPurchaseOrder = purchaseOrderRepository.findById(editPurchaseOrderRequest.getPurchaseOrderId());

        if (existingPurchaseOrder.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrder purchaseOrderToEdit = existingPurchaseOrder.get();
        if (editPurchaseOrderRequest.getSupplierId() != null) purchaseOrderToEdit.setSupplierId(editPurchaseOrderRequest.getSupplierId());
        if (editPurchaseOrderRequest.getOrderDate() != null) purchaseOrderToEdit.setOrderDate(editPurchaseOrderRequest.getOrderDate());
        if (editPurchaseOrderRequest.getExpectedDate() != null) purchaseOrderToEdit.setExpectedDate(editPurchaseOrderRequest.getExpectedDate());
        if (editPurchaseOrderRequest.getExternalId() != null) purchaseOrderToEdit.setExternalId(editPurchaseOrderRequest.getExternalId());
        if (editPurchaseOrderRequest.getNotes() != null) purchaseOrderToEdit.setNotes(editPurchaseOrderRequest.getNotes());
        if (editPurchaseOrderRequest.getPurchaseOrderNumber() != null) purchaseOrderToEdit.setPurchaseOrderNumber(editPurchaseOrderRequest.getPurchaseOrderNumber());
        if (editPurchaseOrderRequest.getUpdatedByUserId() != null) purchaseOrderToEdit.setUpdatedByUserId(editPurchaseOrderRequest.getUpdatedByUserId());

        // NEW FIELDS - Party Information
        if (editPurchaseOrderRequest.getOrganizationId() != null) {
            purchaseOrderToEdit.setOrganizationId(editPurchaseOrderRequest.getOrganizationId());
        }
        if (editPurchaseOrderRequest.getBuyerContact() != null) {
            purchaseOrderToEdit.setBuyerContact(editPurchaseOrderRequest.getBuyerContact());
        }
        if (editPurchaseOrderRequest.getSupplierContact() != null) {
            purchaseOrderToEdit.setSupplierContact(editPurchaseOrderRequest.getSupplierContact());
        }

        // NEW FIELDS - Financial Terms
        if (editPurchaseOrderRequest.getCurrency() != null) {
            purchaseOrderToEdit.setCurrency(editPurchaseOrderRequest.getCurrency());
        }
        if (editPurchaseOrderRequest.getPaymentTerm() != null) {
            purchaseOrderToEdit.setPaymentTerm(editPurchaseOrderRequest.getPaymentTerm());
        }
        if (editPurchaseOrderRequest.getPaymentDueDate() != null) {
            purchaseOrderToEdit.setPaymentDueDate(editPurchaseOrderRequest.getPaymentDueDate());
        }
        if (editPurchaseOrderRequest.getTaxRate() != null) {
            purchaseOrderToEdit.setTaxRate(editPurchaseOrderRequest.getTaxRate());
        }
        if (editPurchaseOrderRequest.getEarlyPaymentDiscountPct() != null) {
            purchaseOrderToEdit.setEarlyPaymentDiscountPct(editPurchaseOrderRequest.getEarlyPaymentDiscountPct());
            if (editPurchaseOrderRequest.getEarlyPaymentDiscountUntil() != null) {
                purchaseOrderToEdit.setEarlyPaymentDiscountUntil(editPurchaseOrderRequest.getEarlyPaymentDiscountUntil());
            }
        }
        if (editPurchaseOrderRequest.getPrepaymentRequired() != null) {
            purchaseOrderToEdit.setPrepaymentRequired(editPurchaseOrderRequest.getPrepaymentRequired());
            if (editPurchaseOrderRequest.getPrepaymentRequired() && editPurchaseOrderRequest.getPrepaymentPercent() != null) {
                purchaseOrderToEdit.setPrepaymentPercent(editPurchaseOrderRequest.getPrepaymentPercent());
            }
        }

        // NEW FIELDS - Shipping & Logistics
        if (editPurchaseOrderRequest.getShipFromLocationId() != null) {
            purchaseOrderToEdit.setShipFromLocationId(editPurchaseOrderRequest.getShipFromLocationId());
        }
        if (editPurchaseOrderRequest.getShipToLocationId() != null) {
            purchaseOrderToEdit.setShipToLocationId(editPurchaseOrderRequest.getShipToLocationId());
        }
        if (editPurchaseOrderRequest.getReceivingWarehouseId() != null) {
            purchaseOrderToEdit.setReceivingWarehouseId(editPurchaseOrderRequest.getReceivingWarehouseId());
        }
        if (editPurchaseOrderRequest.getFreightTerms() != null) {
            purchaseOrderToEdit.setFreightTerms(editPurchaseOrderRequest.getFreightTerms());
        }
        if (editPurchaseOrderRequest.getShipMode() != null) {
            purchaseOrderToEdit.setShipMode(editPurchaseOrderRequest.getShipMode());
        }
        if (editPurchaseOrderRequest.getShippingInstructions() != null) {
            purchaseOrderToEdit.setShippingInstructions(editPurchaseOrderRequest.getShippingInstructions());
        }

        // NEW FIELDS - Import/Export
        if (editPurchaseOrderRequest.getIsImport() != null) {
            purchaseOrderToEdit.setIsImport(editPurchaseOrderRequest.getIsImport());
            if (editPurchaseOrderRequest.getIsImport()) {
                if (editPurchaseOrderRequest.getCustomsDeclarationNumber() != null) {
                    purchaseOrderToEdit.setCustomsDeclarationNumber(editPurchaseOrderRequest.getCustomsDeclarationNumber());
                }
                if (editPurchaseOrderRequest.getPortOfEntry() != null) {
                    purchaseOrderToEdit.setPortOfEntry(editPurchaseOrderRequest.getPortOfEntry());
                }
            } else {
                // Clear import fields if not an import
                purchaseOrderToEdit.setCustomsDeclarationNumber(null);
                purchaseOrderToEdit.setPortOfEntry(null);
            }
        }

        // NEW FIELDS - Approval workflow (if status is being approved/rejected)
        if (editPurchaseOrderRequest.getApprovedByUserId() != null) {
            purchaseOrderToEdit.setApprovedByUserId(editPurchaseOrderRequest.getApprovedByUserId());
            if (purchaseOrderToEdit.getApprovedAt() == null) {
                purchaseOrderToEdit.setApprovedAt(LocalDateTime.now());
            }
        }
        if (editPurchaseOrderRequest.getApprovalNotes() != null) {
            purchaseOrderToEdit.setApprovalNotes(editPurchaseOrderRequest.getApprovalNotes());
        }

        if (editPurchaseOrderRequest.getStatus() != null) {

            if (!isValidStatusTransition(purchaseOrderToEdit.getStatus(), editPurchaseOrderRequest.getStatus())) {

                message = messageService.getMessage(I18Code.MESSAGE_INVALID_STATUS_TRANSITION.getCode(),
                        new String[]{purchaseOrderToEdit.getStatus().name(), editPurchaseOrderRequest.getStatus().name()},
                        locale);

                return buildResponse(400, false, message, null, null, null);
            }

            if (editPurchaseOrderRequest.getStatus() == PurchaseOrderStatus.APPROVED) {

                ValidatorDto approvalValidation = validator.isApprovalValid(editPurchaseOrderRequest, purchaseOrderToEdit,
                        locale);

                if (!approvalValidation.getSuccess()) {

                    message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PURCHASE_ORDER_REQUEST_IS_NULL.getCode(),
                            new String[]{}, locale);

                    return buildResponseWithErrors(400, false, message, null, null,
                            approvalValidation.getErrorMessages());
                }
            }

            purchaseOrderToEdit.setStatus(editPurchaseOrderRequest.getStatus());

            // Send appropriate notifications based on status change
            if (editPurchaseOrderRequest.getStatus() == PurchaseOrderStatus.APPROVED) {
                sendPurchaseOrderApprovedNotification(purchaseOrderToEdit);

                // NEW: Publish event to trigger Sales Order creation
                publishPurchaseOrderApprovedEvent(purchaseOrderToEdit, editPurchaseOrderRequest, locale);

            } else if (editPurchaseOrderRequest.getStatus() == PurchaseOrderStatus.CANCELLED) {
                sendPurchaseOrderCancelledNotification(purchaseOrderToEdit, "Purchase order cancelled");
            }
        }

        if (editPurchaseOrderRequest.getLines() != null) {

            List<PurchaseOrderLine> updatedLines = new ArrayList<>();

            for (EditPurchaseOrderRequest.PurchaseOrderLineUpdateRequest purchaseOrderLineUpdateRequest :
                    editPurchaseOrderRequest.getLines()) {

                Product product = productRepository.findByIdAndEntityStatusNot(purchaseOrderLineUpdateRequest.getProductId(),
                        EntityStatus.DELETED).orElse(null);

                if (product == null) {
                    continue;
                }

                if (purchaseOrderLineUpdateRequest.getPurchaseOrderLineId() != null) {

                    Optional<PurchaseOrderLine> existingLineOpt = purchaseOrderToEdit.getPurchaseOrderLines().stream()
                            .filter(purchaseOrderLine -> purchaseOrderLine.getId().equals(
                                    purchaseOrderLineUpdateRequest.getPurchaseOrderLineId())).findFirst();

                    if (existingLineOpt.isPresent()) {

                        PurchaseOrderLine existingLine = existingLineOpt.get();
                        modelMapper.map(purchaseOrderLineUpdateRequest, existingLine);
                        existingLine.setUpdatedByUserId(editPurchaseOrderRequest.getUpdatedByUserId());
                        updatedLines.add(existingLine);
                    } else {

                        PurchaseOrderLine purchaseOrderLine = modelMapper.map(purchaseOrderLineUpdateRequest, PurchaseOrderLine.class);
                        purchaseOrderLine.setPurchaseOrder(purchaseOrderToEdit);
                        purchaseOrderLine.setProduct(product);
                        purchaseOrderLine.setCreatedByUserId(editPurchaseOrderRequest.getUpdatedByUserId());
                        updatedLines.add(purchaseOrderLine);
                    }
                } else {

                    PurchaseOrderLine purchaseOrderLine = modelMapper.map(purchaseOrderLineUpdateRequest, PurchaseOrderLine.class);
                    purchaseOrderLine.setPurchaseOrder(purchaseOrderToEdit);
                    purchaseOrderLine.setProduct(product);
                    purchaseOrderLine.setCreatedByUserId(editPurchaseOrderRequest.getUpdatedByUserId());
                    updatedLines.add(purchaseOrderLine);
                }
            }

            // ✅ CORRECT: Clear and add instead of replacing the reference
            purchaseOrderToEdit.getPurchaseOrderLines().clear();
            purchaseOrderToEdit.getPurchaseOrderLines().addAll(updatedLines);
        }

        if (editPurchaseOrderRequest.getEntityStatus() != null) purchaseOrderToEdit.setEntityStatus(
                editPurchaseOrderRequest.getEntityStatus());

        // Calculate totals after any updates to lines or tax rate
        purchaseOrderToEdit.calculateTotals();
        applyFunctionalCurrencyConversion(purchaseOrderToEdit);

        PurchaseOrder saved = purchaseOrderServiceAuditable.update(purchaseOrderToEdit, locale, username);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        PurchaseOrderDto purchaseOrderDto = modelMapper.map(saved, PurchaseOrderDto.class);

        String successMsg = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, successMsg, purchaseOrderDto, null, null);
    }

    /**
     * Receive goods - delegates to GoodsReceiptProcessor for business logic
     */
    @Override
    @Transactional
    public PurchaseOrderResponse receiveGoods(ReceiveGoodsRequest receiveGoodsRequest,
                                              String username, Locale locale) {
        log.info("Incoming request to receive goods for PO {} by user {}",
                receiveGoodsRequest.getPurchaseOrderId(), username);

        // Delegate to processor for all business logic
        GoodsReceiptResult result = goodsReceiptProcessor.processGoodsReceipt(
                receiveGoodsRequest, username, locale);

        if (!result.isSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_RECEIVE_GOODS_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            List<String> errors = result.getErrors() == null ? null :
                    result.getErrors().stream()
                            .map(ReceiptValidationError::getErrorMessage)
                            .collect(Collectors.toList());

            return buildResponseWithErrors(400, false, message, null, null, errors);
        }

        // Reload PO to get updated state
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository
                .findByIdAndEntityStatusNot(receiveGoodsRequest.getPurchaseOrderId(), EntityStatus.DELETED);

        if (poOpt.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrder updatedPurchaseOrder = poOpt.get();

        // Load the GRV that was just created
        Optional<GoodsReceivedVoucher> grvOpt = grvRepository
                .findByPurchaseOrderIdAndEntityStatusNot(updatedPurchaseOrder.getId(), EntityStatus.DELETED)
                .stream()
                .max(Comparator.comparing(GoodsReceivedVoucher::getCreatedAt));

        // Send notifications
        if (grvOpt.isPresent()) {
            sendGoodsReceivedNotification(updatedPurchaseOrder, grvOpt.get(),
                    updatedPurchaseOrder.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        // Map to DTO
        PurchaseOrderDto purchaseOrderDto = modelMapper.map(updatedPurchaseOrder, PurchaseOrderDto.class);

        String message = updatedPurchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED ?
                messageService.getMessage(I18Code.MESSAGE_RECEIVE_GOODS_SUCCESSFUL.getCode(), new String[]{}, locale) :
                messageService.getMessage(I18Code.MESSAGE_RECEIVE_GOODS_PARTIALLY_RECEIVED.getCode(), new String[]{}, locale);

        log.info("Successfully processed goods receipt for PO {}",
                updatedPurchaseOrder.getPurchaseOrderNumber());

        return buildResponse(200, true, message, purchaseOrderDto, null, null);
    }

    @Override
    public byte[] exportGrvToPdf(Long grvId) throws DocumentException, IOException {

        Optional<GoodsReceivedVoucher> goodsReceivedVoucher = grvRepository.findByIdAndEntityStatusNot(grvId, EntityStatus.DELETED);

        if (goodsReceivedVoucher.isEmpty()) {
            throw new IllegalArgumentException("Goods Received Voucher not found.");
        }

        GoodsReceivedVoucher grv = goodsReceivedVoucher.get();
        PurchaseOrder purchaseOrder = grv.getPurchaseOrder();

        List<StockTransactionHistory> transactions = stockTransactionHistoryRepository.
                findByReferenceDocumentIdAndReferenceDocumentTypeAndEntityStatusNot(grv.getId(),
                        ReferenceDocumentType.GOODS_RECEIVED_VOUCHER, EntityStatus.DELETED);

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        document.add(new Paragraph("Goods Received Voucher (GRV)", titleFont));
        document.add(new Paragraph("GRV Number: " + grv.getGrvNumber()));
        document.add(new Paragraph("Purchase Order Number: " + purchaseOrder.getPurchaseOrderNumber()));
        document.add(new Paragraph("Received By: " + grv.getReceivedByUserId())); // Assuming this maps to a user
        document.add(new Paragraph("Received At: " + grv.getWarehouseLocation().getName()));
        document.add(new Paragraph("Date: " + grv.getReceivedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        table.addCell(new PdfPCell(new Phrase("Product", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Quantity Received", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Unit Price", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Total Price", headerFont)));

        for (StockTransactionHistory transaction : transactions) {

            PurchaseOrderLine purchaseOrderLine = purchaseOrder.getPurchaseOrderLines().stream()
                    .filter(orderLine -> orderLine.getProduct().getId().
                            equals(transaction.getInventoryItem().getProduct().getId()))
                    .findFirst()
                    .orElse(null);

            if (purchaseOrderLine != null) {

                table.addCell(purchaseOrderLine.getProduct().getName());
                table.addCell(String.valueOf(transaction.getQuantityChange()));
                table.addCell(String.valueOf(purchaseOrderLine.getUnitPrice()));
                table.addCell(String.valueOf(purchaseOrderLine.getUnitPrice().multiply(transaction.getQuantityChange())));
            }
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public PurchaseOrderResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = validator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<PurchaseOrder> existingOpt = purchaseOrderRepository.findById(id);

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrder purchaseOrderToDelete = existingOpt.get();
        purchaseOrderToDelete.setEntityStatus(EntityStatus.DELETED);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        PurchaseOrder saved = purchaseOrderServiceAuditable.delete(purchaseOrderToDelete, locale);

        // Send cancellation notification
        sendPurchaseOrderCancelledNotification(saved, "Purchase order deleted");

        PurchaseOrderDto purchaseOrderDto = modelMapper.map(saved, PurchaseOrderDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_DELETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, purchaseOrderDto, null, null);
    }

    @Override
    public PurchaseOrderResponse findByMultipleFilters(PurchaseOrderMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<PurchaseOrder> spec = PurchaseOrderSpecification.deleted();

        if (request == null || request.getPage() < 0 || request.getSize() <= 0) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_PURCHASE_ORDER_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale);

            return buildResponseWithErrors(400, false, message, null, null,
                    null);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (request.getPurchaseOrderNumber() != null && !request.getPurchaseOrderNumber().isBlank()) {
            spec = spec.and(PurchaseOrderSpecification.purchaseOrderNumberLike(request.getPurchaseOrderNumber()));
        }

        if (request.getExternalId() != null && !request.getExternalId().isBlank()) {
            spec = spec.and(PurchaseOrderSpecification.externalIdLike(request.getExternalId()));
        }

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            spec = spec.and(PurchaseOrderSpecification.notesLike(request.getNotes()));
        }

        if (request.getStatus() != null) {
            spec = spec.and(PurchaseOrderSpecification.statusEquals(request.getStatus()));
        }

        if (request.getOrderDate() != null) {
            spec = spec.and(PurchaseOrderSpecification.orderDateEquals(request.getOrderDate()));
        }

        if (request.getExpectedDate() != null) {
            spec = spec.and(PurchaseOrderSpecification.expectedDateEquals(request.getExpectedDate()));
        }

        if (request.getEntityStatus() != null) {
            spec = spec.and(PurchaseOrderSpecification.entityStatusEquals(request.getEntityStatus()));
        }

        if (request.getSearchValue() != null && !request.getSearchValue().isBlank()) {
            spec = spec.and(PurchaseOrderSpecification.any(request.getSearchValue()));
        }

        long totalCount = purchaseOrderRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage && totalCount > 0) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{},
                    locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Page<PurchaseOrder> result = purchaseOrderRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildResponse(404, false, message, null, null, null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        Page<PurchaseOrderDto> purchaseOrderDtoPage = result.map(purchaseOrder -> modelMapper.map(purchaseOrder,
                PurchaseOrderDto.class));

        message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);
        PurchaseOrderResponse response = buildResponse(200, true, message, null, null, null);
        response.setPurchaseOrderDtoPage(purchaseOrderDtoPage);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse searchForPlatformDashboard(String term, int limit, Locale locale) {
        if (term == null || term.isBlank()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);
            PurchaseOrderResponse response = buildResponse(200, true, message, null, null, null);
            response.setPurchaseOrderDtoList(List.of());
            return response;
        }

        int capped = Math.max(1, Math.min(limit, 50));
        Specification<PurchaseOrder> spec = PurchaseOrderSpecification.deleted()
                .and(PurchaseOrderSpecification.any(term.trim()));
        Page<PurchaseOrder> result = purchaseOrderRepository.findAll(spec, PageRequest.of(0, capped));

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<PurchaseOrderDto> rows = result.getContent().stream()
                .map(po -> modelMapper.map(po, PurchaseOrderDto.class))
                .toList();

        String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        PurchaseOrderResponse response = buildResponse(200, true, message, null, null, null);
        response.setPurchaseOrderDtoList(rows);
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    /**
     * Generate a unique GRV number
     * Format: GRV-yyyyMMdd-HHmmss-XXXX (where XXXX is last 4 digits of millis)
     */
    private String generateGrvNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String millis = String.valueOf(System.currentTimeMillis());
        String suffix = millis.substring(millis.length() - 4);
        return "GRV-" + datePart + "-" + suffix;
    }

    @Override
    public byte[] exportToCsv(List<PurchaseOrderDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (PurchaseOrderDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(safe(item.getPurchaseOrderNumber())).append(",")
                    .append(item.getSupplierId() != null ? item.getSupplierId() : "").append(",")
                    .append(item.getStatus() != null ? item.getStatus().name() : "").append(",")
                    .append(item.getOrderDate() != null ? item.getOrderDate() : "").append(",")
                    .append(item.getExpectedDate() != null ? item.getExpectedDate() : "").append(",")
                    .append(item.getReceivedDate() != null ? item.getReceivedDate() : "").append(",")
                    .append(safe(item.getExternalId())).append(",")
                    .append(safe(item.getNotes()))
                    .append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<PurchaseOrderDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchase Orders");
        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (PurchaseOrderDto item : items) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(safe(item.getPurchaseOrderNumber()));
            row.createCell(2).setCellValue(item.getSupplierId() != null ? item.getSupplierId() : 0);
            row.createCell(3).setCellValue(item.getStatus() != null ? item.getStatus().name() : "");
            row.createCell(4).setCellValue(item.getOrderDate() != null ? item.getOrderDate().toString() : "");
            row.createCell(5).setCellValue(item.getExpectedDate() != null ? item.getExpectedDate().toString() : "");
            row.createCell(6).setCellValue(item.getReceivedDate() != null ? item.getReceivedDate().toString() : "");
            row.createCell(7).setCellValue(safe(item.getExternalId()));
            row.createCell(8).setCellValue(safe(item.getNotes()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<PurchaseOrderDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (PurchaseOrderDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    safe(item.getPurchaseOrderNumber()),
                    String.valueOf(item.getSupplierId() != null ? item.getSupplierId() : 0),
                    item.getStatus() != null ? item.getStatus().name() : "",
                    item.getOrderDate() != null ? item.getOrderDate().toString() : "",
                    item.getExpectedDate() != null ? item.getExpectedDate().toString() : "",
                    item.getReceivedDate() != null ? item.getReceivedDate().toString() : "",
                    safe(item.getExternalId()),
                    safe(item.getNotes())
            });
        }
        return InventoryExportSupport.writeTabularPdf("Purchase Orders", "INV-POD",
                "Purchase order export", HEADERS, rows, true);
    }

    @Override
    public ImportSummary importPurchaseOrderFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;
        try (java.io.Reader reader = new java.io.InputStreamReader(csvInputStream, java.nio.charset.StandardCharsets.UTF_8);
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

                    CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
                    String supplierIdStr = record.isMapped("SUPPLIER_ID") ? record.get("SUPPLIER_ID") : null;

                    if (supplierIdStr != null && !supplierIdStr.isBlank()) {
                        request.setSupplierId(Long.parseLong(supplierIdStr.trim()));
                    }

                    String externalId = record.isMapped("EXTERNAL_ID") ? record.get("EXTERNAL_ID") : null;
                    request.setExternalId(externalId);
                    String orderDateStr = record.isMapped("ORDER_DATE") ? record.get("ORDER_DATE") : null;

                    if (orderDateStr != null && !orderDateStr.isBlank()) {
                        request.setOrderDate(LocalDate.parse(orderDateStr.trim()));
                    }

                    String expectedDateStr = record.isMapped("EXPECTED_DATE") ? record.get("EXPECTED_DATE") : null;

                    if (expectedDateStr != null && !expectedDateStr.isBlank()) {
                        request.setExpectedDate(LocalDate.parse(expectedDateStr.trim()));
                    }

                    String notes = record.isMapped("NOTES") ? record.get("NOTES") : null;
                    request.setNotes(notes);
                    PurchaseOrderResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
                ? "Import completed successfully. " + success + " out of " + total + " purchase orders imported."
                : "Import failed. No purchase orders were imported.";
        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private PurchaseOrderResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                                PurchaseOrderDto dto, List<PurchaseOrderDto> dtoList,
                                                List<String> errorMessages) {
        PurchaseOrderResponse response = new PurchaseOrderResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setPurchaseOrderDto(dto);
        response.setPurchaseOrderDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private PurchaseOrderResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                          PurchaseOrderDto dto, List<PurchaseOrderDto> dtoList,
                                                          List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }

    private String generatePurchaseOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String millis = String.valueOf(System.currentTimeMillis());
        String suffix = millis.substring(millis.length() - 4);
        return "PO-" + datePart + "-" + suffix;
    }

    private boolean isValidStatusTransition(PurchaseOrderStatus currentStatus, PurchaseOrderStatus newStatus) {

        if (currentStatus == newStatus) {
            return true; // No change
        }

        return switch (currentStatus) {
            case DRAFT -> newStatus == PurchaseOrderStatus.SUBMITTED
                    || newStatus == PurchaseOrderStatus.CANCELLED;
            // SUBMITTED can go directly to APPROVED (legacy single-stage) or through
            // the multi-stage workflow: PENDING_CUSTOMER_APPROVAL → CUSTOMER_APPROVED → PENDING_SUPPLIER_APPROVAL → APPROVED
            case SUBMITTED -> newStatus == PurchaseOrderStatus.APPROVED
                    || newStatus == PurchaseOrderStatus.PENDING_CUSTOMER_APPROVAL
                    || newStatus == PurchaseOrderStatus.REJECTED
                    || newStatus == PurchaseOrderStatus.CANCELLED;
            case PENDING_CUSTOMER_APPROVAL -> newStatus == PurchaseOrderStatus.CUSTOMER_APPROVED
                    || newStatus == PurchaseOrderStatus.REJECTED
                    || newStatus == PurchaseOrderStatus.CANCELLED;
            case CUSTOMER_APPROVED -> newStatus == PurchaseOrderStatus.PENDING_SUPPLIER_APPROVAL
                    || newStatus == PurchaseOrderStatus.CANCELLED;
            case PENDING_SUPPLIER_APPROVAL -> newStatus == PurchaseOrderStatus.APPROVED
                    || newStatus == PurchaseOrderStatus.REJECTED
                    || newStatus == PurchaseOrderStatus.CANCELLED;
            case APPROVED -> newStatus == PurchaseOrderStatus.PARTIALLY_RECEIVED
                    || newStatus == PurchaseOrderStatus.RECEIVED
                    || newStatus == PurchaseOrderStatus.CANCELLED;
            case PARTIALLY_RECEIVED -> newStatus == PurchaseOrderStatus.RECEIVED;
            case RECEIVED, CANCELLED, REJECTED -> false; // Terminal states
        };
    }

    /**
     * Helper method to fetch supplier
     */
    private OrganizationResponse getSupplierDetails(Long supplierId) {

        OrganizationResponse supplierResponse = new OrganizationResponse();

        try {
            // Fetch supplier details
            supplierResponse = organizationServiceClient.findById(supplierId, Locale.ENGLISH);

        } catch (Exception e) {
            log.error("Failed to fetch supplier for supplier ID: {}. Error: {}",
                    supplierId, e.getMessage());
        }

        return supplierResponse;
    }

    /**
     * Helper method to fetch customer
     */
    private OrganizationResponse getCustomerDetails(Long customerId) {

        OrganizationResponse customerResponse = new OrganizationResponse();

        try {
            // Fetch customer details
            UserResponse userResponse = userManagementServiceClient.findById(customerId, Locale.ENGLISH);

            customerResponse = organizationServiceClient.findById(userResponse.getUserDto().getOrganizationId(),
                    Locale.ENGLISH);

        } catch (Exception e) {
            log.error("Failed to fetch customer for customer ID: {}. Error: {}",
                    customerId, e.getMessage());
        }

        return customerResponse;
    }

    /**
     * Send a notification when a new purchase order is created
     */
    private void sendPurchaseOrderCreatedNotification(PurchaseOrder purchaseOrder) {

        try {

            // Send it to the supplier
            sendPurchaseOrderCreatedToSupplier(purchaseOrder);

            // Send it to a customer/internal team
            sendPurchaseOrderCreatedToCustomer(purchaseOrder);

            log.info("Successfully sent purchase order created notifications for PO: {}",
                    purchaseOrder.getPurchaseOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send purchase order created notification for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send purchase order created notification specifically to the supplier
     */
    private void sendPurchaseOrderCreatedToSupplier(PurchaseOrder purchaseOrder) {

        try {
            Map<String, Object> supplierData = Map.ofEntries(
                    Map.entry("purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber()),
                    Map.entry("orderDate", purchaseOrder.getOrderDate() != null ? purchaseOrder.getOrderDate().toString() : ""),
                    Map.entry("expectedDate", purchaseOrder.getExpectedDate() != null ? purchaseOrder.getExpectedDate().toString() : ""),
                    Map.entry("totalLines", purchaseOrder.getPurchaseOrderLines() != null ? purchaseOrder.getPurchaseOrderLines().size() : 0),
                    Map.entry("notes", purchaseOrder.getNotes() != null ? purchaseOrder.getNotes() : ""),
                    Map.entry("externalId", purchaseOrder.getExternalId() != null ? purchaseOrder.getExternalId() : ""),
                    Map.entry("totalAmount", purchaseOrder.getTotalAmount() != null ? purchaseOrder.getTotalAmount().toString() : "0"),
                    Map.entry("currency", purchaseOrder.getCurrency() != null ? purchaseOrder.getCurrency() : "USD"),
                    Map.entry("freightTerms", purchaseOrder.getFreightTerms() != null ? purchaseOrder.getFreightTerms().name() : ""),
                    Map.entry("shipMode", purchaseOrder.getShipMode() != null ? purchaseOrder.getShipMode().name() : ""),
                    Map.entry("isImport", purchaseOrder.getIsImport() != null && purchaseOrder.getIsImport()),
                    Map.entry("prepaymentRequired", purchaseOrder.getPrepaymentRequired() != null && purchaseOrder.getPrepaymentRequired())
            );

            // Get supplier contact information
            OrganizationResponse supplierResponse = getSupplierDetails(purchaseOrder.getSupplierId());

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
                            "PURCHASE_ORDER_CREATED_SUPPLIER_EMAIL", // Supplier-specific template
                            supplierEmailRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=EMAIL to={} po={}",
                                "PURCHASE_ORDER_CREATED_SUPPLIER_EMAIL",
                                supplierResponse.getOrganizationDto().getEmail(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierEmailNotification);
                        log.info("Notification dispatched template={} channel=EMAIL po={} to={}",
                                "PURCHASE_ORDER_CREATED_SUPPLIER_EMAIL",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=EMAIL po={} to={}. Error: {}",
                                "PURCHASE_ORDER_CREATED_SUPPLIER_EMAIL",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping EMAIL notification template={} for PO {}: no supplier email available",
                            "PURCHASE_ORDER_CREATED_SUPPLIER_EMAIL",
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // Send SMS/WhatsApp to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null &&
                        !supplierResponse.getOrganizationDto().getPhoneNumber().isBlank()) {

                    NotificationRequest.Recipient supplierPhoneRecipient = new NotificationRequest.Recipient(
                            null, // External recipient
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest supplierPhoneNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_CREATED_SUPPLIER_SMS", // Supplier-specific SMS template
                            supplierPhoneRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=SMS to={} po={}",
                                "PURCHASE_ORDER_CREATED_SUPPLIER_SMS",
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierPhoneNotification);
                        log.info("Notification dispatched template={} channel=SMS po={} to={}",
                                "PURCHASE_ORDER_CREATED_SUPPLIER_SMS",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=SMS po={} to={}. Error: {}",
                                "PURCHASE_ORDER_CREATED_SUPPLIER_SMS",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping SMS notification template={} for PO {}: no supplier phone number available",
                            "PURCHASE_ORDER_CREATED_SUPPLIER_SMS",
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // Send WhatsApp to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null &&
                        !supplierResponse.getOrganizationDto().getPhoneNumber().isBlank()) {

                    NotificationRequest.Recipient supplierPhoneRecipient = new NotificationRequest.Recipient(
                            null, // External recipient
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest supplierPhoneNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_CREATED_SUPPLIER_WHATSAPP", // Supplier-specific SMS template
                            supplierPhoneRecipient,
                            supplierData,
                            null
                    );

                    rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierPhoneNotification);
                }
            }

            log.info("Sent purchase order created notification to supplier for PO: {}",
                    purchaseOrder.getPurchaseOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send purchase order created notification to supplier for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send purchase order created notification to customer/internal team
     */
    private void sendPurchaseOrderCreatedToCustomer(PurchaseOrder purchaseOrder) {
        try {
            Map<String, Object> customerData = Map.of(
                    "purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber(),
                    "supplierId", purchaseOrder.getSupplierId(),
                    "orderDate", purchaseOrder.getOrderDate() != null ? purchaseOrder.getOrderDate().toString() : "",
                    "expectedDate", purchaseOrder.getExpectedDate() != null ? purchaseOrder.getExpectedDate().toString() : "",
                    "totalLines", purchaseOrder.getPurchaseOrderLines() != null ? purchaseOrder.getPurchaseOrderLines().size() : 0,
                    "status", purchaseOrder.getStatus().name(),
                    "totalValue", calculateTotalOrderValue(purchaseOrder) // Add order total for internal tracking
            );

            // Get customer/creator contact information
            OrganizationResponse customerResponse = getCustomerDetails(purchaseOrder.getCreatedByUserId());

            // Notify the original user (purchasing team) - Internal notification
            NotificationRequest.Recipient internalRecipient = new NotificationRequest.Recipient(
                    purchaseOrder.getCreatedByUserId().toString(),
                    customerResponse != null && customerResponse.getOrganizationDto() != null ?
                            customerResponse.getOrganizationDto().getEmail() : null,
                    customerResponse != null && customerResponse.getOrganizationDto() != null ?
                            customerResponse.getOrganizationDto().getPhoneNumber() : null,
                    null
            );

            NotificationRequest internalNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "PURCHASE_ORDER_CREATED_CUSTOMER_INTERNAL", // Customer-specific template
                    internalRecipient,
                    customerData,
                    null
            );

            try {
                log.info("Dispatching notification template={} channel=INTERNAL toUserId={} po={}",
                        "PURCHASE_ORDER_CREATED_CUSTOMER_INTERNAL",
                        purchaseOrder.getCreatedByUserId(),
                        purchaseOrder.getPurchaseOrderNumber());
                rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", internalNotification);
                log.info("Notification dispatched template={} channel=INTERNAL po={} toUserId={}",
                        "PURCHASE_ORDER_CREATED_CUSTOMER_INTERNAL",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId());
            } catch (Exception ex) {
                log.error("Failed to dispatch template={} channel=INTERNAL po={} toUserId={}. Error: {}",
                        "PURCHASE_ORDER_CREATED_CUSTOMER_INTERNAL",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId(),
                        ex.getMessage());
            }

            log.info("Sent purchase order created notification to customer for PO: {}",
                    purchaseOrder.getPurchaseOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send purchase order created notification to customer for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Helper method to calculate total order value
     */
    private BigDecimal calculateTotalOrderValue(PurchaseOrder purchaseOrder) {

        // Use the calculated totalAmount field if available (includes tax)
        if (purchaseOrder.getTotalAmount() != null &&
                purchaseOrder.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            return purchaseOrder.getTotalAmount();
        }

        // Fallback to line-by-line calculation if totalAmount not set
        if (purchaseOrder.getPurchaseOrderLines() == null || purchaseOrder.getPurchaseOrderLines().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return purchaseOrder.getPurchaseOrderLines().stream()
                .map(line -> line.getUnitPrice().multiply(line.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Send notification when purchase order is approved
     */
    private void sendPurchaseOrderApprovedNotification(PurchaseOrder purchaseOrder) {

        try {
            sendPurchaseOrderApprovedToSupplier(purchaseOrder);
            sendPurchaseOrderApprovedToCustomer(purchaseOrder);

            log.info("Successfully sent purchase order approved notifications for PO: {}",
                    purchaseOrder.getPurchaseOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send purchase order approved notification for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    private void sendPurchaseOrderApprovedToSupplier(PurchaseOrder purchaseOrder) {
        try {
            Map<String, Object> supplierData = Map.of(
                    "purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber(),
                    "approvedDate", LocalDateTime.now().toString(),
                    "expectedDate", purchaseOrder.getExpectedDate() != null ? purchaseOrder.getExpectedDate().toString() : "",
                    "totalValue", calculateTotalOrderValue(purchaseOrder),
                    "deliveryInstructions", purchaseOrder.getNotes() != null ? purchaseOrder.getNotes() : ""
            );

            OrganizationResponse supplierResponse = getSupplierDetails(purchaseOrder.getSupplierId());

            if (supplierResponse != null && supplierResponse.getOrganizationDto() != null) {

                // Email notification to supplier
                if (supplierResponse.getOrganizationDto().getEmail() != null) {
                    NotificationRequest.Recipient supplierRecipient = new NotificationRequest.Recipient(
                            null,
                            supplierResponse.getOrganizationDto().getEmail(),
                            null,
                            null
                    );

                    NotificationRequest notification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_APPROVED_SUPPLIER_EMAIL", // Supplier-specific approval template
                            supplierRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=EMAIL to={} po={}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_EMAIL",
                                supplierResponse.getOrganizationDto().getEmail(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notification);
                        log.info("Notification dispatched template={} channel=EMAIL po={} to={}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_EMAIL",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=EMAIL po={} to={}. Error: {}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_EMAIL",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping EMAIL notification template={} for PO {}: no supplier email available",
                            "PURCHASE_ORDER_APPROVED_SUPPLIER_EMAIL",
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // SMS notification to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null) {
                    NotificationRequest.Recipient supplierSmsRecipient = new NotificationRequest.Recipient(
                            null,
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest smsNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_APPROVED_SUPPLIER_SMS",
                            supplierSmsRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=SMS to={} po={}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_SMS",
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", smsNotification);
                        log.info("Notification dispatched template={} channel=SMS po={} to={}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_SMS",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=SMS po={} to={}. Error: {}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_SMS",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping SMS notification template={} for PO {}: no supplier phone number available",
                            "PURCHASE_ORDER_APPROVED_SUPPLIER_SMS",
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // WhatsApp notification to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null) {
                    NotificationRequest.Recipient supplierWhatsAppRecipient = new NotificationRequest.Recipient(
                            null,
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest whatsAppNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_APPROVED_SUPPLIER_WHATSAPP",
                            supplierWhatsAppRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=WHATSAPP to={} po={}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_WHATSAPP",
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", whatsAppNotification);
                        log.info("Notification dispatched template={} channel=WHATSAPP po={} to={}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_WHATSAPP",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=WHATSAPP po={} to={}. Error: {}",
                                "PURCHASE_ORDER_APPROVED_SUPPLIER_WHATSAPP",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping WHATSAPP notification template={} for PO {}: no supplier phone number available",
                            "PURCHASE_ORDER_APPROVED_SUPPLIER_WHATSAPP",
                            purchaseOrder.getPurchaseOrderNumber());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send purchase order approved notification to supplier for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    private void sendPurchaseOrderApprovedToCustomer(PurchaseOrder purchaseOrder) {
        try {
            Map<String, Object> customerData = Map.of(
                    "purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber(),
                    "supplierId", purchaseOrder.getSupplierId(),
                    "approvedDate", LocalDateTime.now().toString(),
                    "expectedDate", purchaseOrder.getExpectedDate() != null ? purchaseOrder.getExpectedDate().toString() : "",
                    "approvedByUserId", purchaseOrder.getUpdatedByUserId() != null ? purchaseOrder.getUpdatedByUserId().toString() : "System"
            );

            // Notify the creator
            NotificationRequest.Recipient creatorRecipient = new NotificationRequest.Recipient(
                    purchaseOrder.getCreatedByUserId().toString(),
                    null, // Will be fetched by notification service
                    null,
                    null
            );

            NotificationRequest creatorNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "PURCHASE_ORDER_APPROVED_CUSTOMER_INTERNAL", // Customer-specific approval template
                    creatorRecipient,
                    customerData,
                    null
            );

            try {
                log.info("Dispatching notification template={} channel=INTERNAL toUserId={} po={}",
                        "PURCHASE_ORDER_APPROVED_CUSTOMER_INTERNAL",
                        purchaseOrder.getCreatedByUserId(),
                        purchaseOrder.getPurchaseOrderNumber());
                rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", creatorNotification);
                log.info("Notification dispatched template={} channel=INTERNAL po={} toUserId={}",
                        "PURCHASE_ORDER_APPROVED_CUSTOMER_INTERNAL",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId());
            } catch (Exception ex) {
                log.error("Failed to dispatch template={} channel=INTERNAL po={} toUserId={}. Error: {}",
                        "PURCHASE_ORDER_APPROVED_CUSTOMER_INTERNAL",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId(),
                        ex.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to send purchase order approved notification to customer for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send notification when goods are received
     */
    private void sendGoodsReceivedNotification(PurchaseOrder purchaseOrder, GoodsReceivedVoucher grv,
                                               boolean isPartiallyReceived) {
        try {

            // Send to supplier
            sendGoodsReceivedToSupplier(purchaseOrder, grv, isPartiallyReceived);

            // Send to customer/internal team
            sendGoodsReceivedToCustomer(purchaseOrder, grv, isPartiallyReceived);

            log.info("Successfully sent goods received notifications for PO: {} ({})",
                    purchaseOrder.getPurchaseOrderNumber(), isPartiallyReceived ? "PARTIAL" : "FULL");
        } catch (Exception e) {
            log.error("Failed to send goods received notification for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send goods received notification specifically to the supplier
     */
    private void sendGoodsReceivedToSupplier(PurchaseOrder purchaseOrder, GoodsReceivedVoucher grv, boolean isPartiallyReceived) {

        try {
            Map<String, Object> supplierData = Map.of(
                    "purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber(),
                    "grvNumber", grv.getGrvNumber(),
                    "warehouseLocation", grv.getWarehouseLocation().getName(),
                    "receivedDate", grv.getReceivedDate().toString(),
                    "status", isPartiallyReceived ? "PARTIALLY_RECEIVED" : "FULLY_RECEIVED",
                    "remainingItems", isPartiallyReceived ? "Some items still pending delivery" : "All items received",
                    "nextAction", isPartiallyReceived ? "Please deliver remaining items" : "Delivery complete - thank you"
            );

            // Get supplier contact information
            OrganizationResponse supplierResponse = getSupplierDetails(purchaseOrder.getSupplierId());

            if (supplierResponse != null && supplierResponse.getOrganizationDto() != null) {
                String emailTemplateKey = isPartiallyReceived ? "GOODS_RECEIVED_PARTIAL_SUPPLIER_EMAIL" : "GOODS_RECEIVED_FULL_SUPPLIER_EMAIL";
                String smsTemplateKey = isPartiallyReceived ? "GOODS_RECEIVED_PARTIAL_SUPPLIER_SMS" : "GOODS_RECEIVED_FULL_SUPPLIER_SMS";
                String whatsappTemplateKey = isPartiallyReceived ? "GOODS_RECEIVED_PARTIAL_SUPPLIER_WHATSAPP" : "GOODS_RECEIVED_FULL_SUPPLIER_WHATSAPP";

                // Send email to supplier
                if (supplierResponse.getOrganizationDto().getEmail() != null &&
                        !supplierResponse.getOrganizationDto().getEmail().isBlank()) {

                    NotificationRequest.Recipient supplierEmailRecipient = new NotificationRequest.Recipient(
                            null,
                            supplierResponse.getOrganizationDto().getEmail(),
                            null,
                            null
                    );

                    NotificationRequest supplierEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            emailTemplateKey,
                            supplierEmailRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=EMAIL to={} po={}",
                                emailTemplateKey,
                                supplierResponse.getOrganizationDto().getEmail(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierEmailNotification);
                        log.info("Notification dispatched template={} channel=EMAIL po={} to={}",
                                emailTemplateKey,
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=EMAIL po={} to={}. Error: {}",
                                emailTemplateKey,
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping EMAIL notification template={} for PO {}: no supplier email available",
                            emailTemplateKey,
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // Send SMS to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null &&
                        !supplierResponse.getOrganizationDto().getPhoneNumber().isBlank()) {

                    NotificationRequest.Recipient supplierSmsRecipient = new NotificationRequest.Recipient(
                            null,
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest supplierSmsNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            smsTemplateKey,
                            supplierSmsRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=SMS to={} po={}",
                                smsTemplateKey,
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierSmsNotification);
                        log.info("Notification dispatched template={} channel=SMS po={} to={}",
                                smsTemplateKey,
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=SMS po={} to={}. Error: {}",
                                smsTemplateKey,
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping SMS notification template={} for PO {}: no supplier phone number available",
                            smsTemplateKey,
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // Send WhatsApp to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null &&
                        !supplierResponse.getOrganizationDto().getPhoneNumber().isBlank()) {

                    NotificationRequest.Recipient supplierWhatsAppRecipient = new NotificationRequest.Recipient(
                            null,
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest supplierWhatsAppNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            whatsappTemplateKey,
                            supplierWhatsAppRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=WHATSAPP to={} po={}",
                                whatsappTemplateKey,
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierWhatsAppNotification);
                        log.info("Notification dispatched template={} channel=WHATSAPP po={} to={}",
                                whatsappTemplateKey,
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=WHATSAPP po={} to={}. Error: {}",
                                whatsappTemplateKey,
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping WHATSAPP notification template={} for PO {}: no supplier phone number available",
                            whatsappTemplateKey,
                            purchaseOrder.getPurchaseOrderNumber());
                }
            }

            log.info("Sent goods received notification to supplier for PO: {} ({})",
                    purchaseOrder.getPurchaseOrderNumber(), isPartiallyReceived ? "PARTIAL" : "FULL");
        } catch (Exception e) {
            log.error("Failed to send goods received notification to supplier for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send goods received notification to customer/internal team
     */
    private void sendGoodsReceivedToCustomer(PurchaseOrder purchaseOrder, GoodsReceivedVoucher grv, boolean isPartiallyReceived) {

        try {
            Map<String, Object> customerData = Map.of(
                    "purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber(),
                    "grvNumber", grv.getGrvNumber(),
                    "warehouseLocation", grv.getWarehouseLocation().getName(),
                    "receivedByUserId", grv.getReceivedByUserId(),
                    "receivedDate", grv.getReceivedDate().toString(),
                    "status", isPartiallyReceived ? "PARTIALLY_RECEIVED" : "FULLY_RECEIVED",
                    "supplierId", purchaseOrder.getSupplierId(),
                    "completionPercentage", isPartiallyReceived ? "Partially complete" : "100% complete",
                    "receivedValue", calculateReceivedValue(purchaseOrder, grv)
            );

            String internalTemplateKey = isPartiallyReceived ? "GOODS_RECEIVED_PARTIAL_CUSTOMER_INTERNAL" :
                    "GOODS_RECEIVED_FULL_CUSTOMER_INTERNAL";

            // Notify the original user (purchasing team) - Internal notification
            NotificationRequest.Recipient internalRecipient = new NotificationRequest.Recipient(
                    purchaseOrder.getCreatedByUserId().toString(),
                    null, // Will be fetched by notification service
                    null,
                    null
            );

            NotificationRequest internalNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    internalTemplateKey,
                    internalRecipient,
                    customerData,
                    null
            );

            try {
                log.info("Dispatching notification template={} channel=INTERNAL toUserId={} po={}",
                        internalTemplateKey,
                        purchaseOrder.getCreatedByUserId(),
                        purchaseOrder.getPurchaseOrderNumber());
                rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", internalNotification);
                log.info("Notification dispatched template={} channel=INTERNAL po={} toUserId={}",
                        internalTemplateKey,
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId());
            } catch (Exception ex) {
                log.error("Failed to dispatch template={} channel=INTERNAL po={} toUserId={}. Error: {}",
                        internalTemplateKey,
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId(),
                        ex.getMessage());
            }

            log.info("Sent goods received notification to customer for PO: {} ({})",
                    purchaseOrder.getPurchaseOrderNumber(), isPartiallyReceived ? "PARTIAL" : "FULL");
        } catch (Exception e) {
            log.error("Failed to send goods received notification to customer for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send notification when purchase order is cancelled
     */
    private void sendPurchaseOrderCancelledNotification(PurchaseOrder purchaseOrder, String reason) {

        try {
            // Send to supplier
            sendPurchaseOrderCancelledToSupplier(purchaseOrder, reason);

            // Send to customer/internal team
            sendPurchaseOrderCancelledToCustomer(purchaseOrder, reason);

            log.info("Successfully sent purchase order cancelled notifications for PO: {}",
                    purchaseOrder.getPurchaseOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send purchase order cancelled notification for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send purchase order cancelled notification specifically to the supplier
     */
    private void sendPurchaseOrderCancelledToSupplier(PurchaseOrder purchaseOrder, String reason) {

        try {
            Map<String, Object> supplierData = Map.of(
                    "purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber(),
                    "cancelledDate", LocalDateTime.now().toString(),
                    "reason", reason != null ? reason : "No reason provided",
                    "orderDate", purchaseOrder.getOrderDate() != null ? purchaseOrder.getOrderDate().toString() : "",
                    "orderValue", calculateTotalOrderValue(purchaseOrder),
                    "apologyMessage", "We apologize for any inconvenience caused by this cancellation"
            );

            // Get supplier contact information
            OrganizationResponse supplierResponse = getSupplierDetails(purchaseOrder.getSupplierId());

            if (supplierResponse != null && supplierResponse.getOrganizationDto() != null) {
                // Send email to supplier
                if (supplierResponse.getOrganizationDto().getEmail() != null &&
                        !supplierResponse.getOrganizationDto().getEmail().isBlank()) {

                    NotificationRequest.Recipient supplierEmailRecipient = new NotificationRequest.Recipient(
                            null,
                            supplierResponse.getOrganizationDto().getEmail(),
                            null,
                            null
                    );

                    NotificationRequest supplierEmailNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_CANCELLED_SUPPLIER_EMAIL",
                            supplierEmailRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=EMAIL to={} po={}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_EMAIL",
                                supplierResponse.getOrganizationDto().getEmail(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierEmailNotification);
                        log.info("Notification dispatched template={} channel=EMAIL po={} to={}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_EMAIL",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=EMAIL po={} to={}. Error: {}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_EMAIL",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getEmail(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping EMAIL notification template={} for PO {}: no supplier email available",
                            "PURCHASE_ORDER_CANCELLED_SUPPLIER_EMAIL",
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // Send SMS to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null &&
                        !supplierResponse.getOrganizationDto().getPhoneNumber().isBlank()) {

                    NotificationRequest.Recipient supplierSmsRecipient = new NotificationRequest.Recipient(
                            null,
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest supplierSmsNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_CANCELLED_SUPPLIER_SMS",
                            supplierSmsRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=SMS to={} po={}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_SMS",
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                                supplierSmsNotification);
                        log.info("Notification dispatched template={} channel=SMS po={} to={}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_SMS",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=SMS po={} to={}. Error: {}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_SMS",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping SMS notification template={} for PO {}: no supplier phone number available",
                            "PURCHASE_ORDER_CANCELLED_SUPPLIER_SMS",
                            purchaseOrder.getPurchaseOrderNumber());
                }

                // Send WhatsApp to supplier
                if (supplierResponse.getOrganizationDto().getPhoneNumber() != null &&
                        !supplierResponse.getOrganizationDto().getPhoneNumber().isBlank()) {

                    NotificationRequest.Recipient supplierWhatsAppRecipient = new NotificationRequest.Recipient(
                            null,
                            null,
                            supplierResponse.getOrganizationDto().getPhoneNumber(),
                            null
                    );

                    NotificationRequest supplierWhatsAppNotification = new NotificationRequest(
                            UUID.randomUUID().toString(),
                            "PURCHASE_ORDER_CANCELLED_SUPPLIER_WHATSAPP",
                            supplierWhatsAppRecipient,
                            supplierData,
                            null
                    );

                    try {
                        log.info("Dispatching notification template={} channel=WHATSAPP to={} po={}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_WHATSAPP",
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                purchaseOrder.getPurchaseOrderNumber());
                        rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", supplierWhatsAppNotification);
                        log.info("Notification dispatched template={} channel=WHATSAPP po={} to={}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_WHATSAPP",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber());
                    } catch (Exception ex) {
                        log.error("Failed to dispatch template={} channel=WHATSAPP po={} to={}. Error: {}",
                                "PURCHASE_ORDER_CANCELLED_SUPPLIER_WHATSAPP",
                                purchaseOrder.getPurchaseOrderNumber(),
                                supplierResponse.getOrganizationDto().getPhoneNumber(),
                                ex.getMessage());
                    }
                } else {
                    log.info("Skipping WHATSAPP notification template={} for PO {}: no supplier phone number available",
                            "PURCHASE_ORDER_CANCELLED_SUPPLIER_WHATSAPP",
                            purchaseOrder.getPurchaseOrderNumber());
                }
            }

            log.info("Sent purchase order cancelled notification to supplier for PO: {}",
                    purchaseOrder.getPurchaseOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send purchase order cancelled notification to supplier for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Send purchase order cancelled notification to customer/internal team
     */
    private void sendPurchaseOrderCancelledToCustomer(PurchaseOrder purchaseOrder, String reason) {
        try {
            Map<String, Object> customerData = Map.of(
                    "purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber(),
                    "supplierId", purchaseOrder.getSupplierId(),
                    "cancelledDate", LocalDateTime.now().toString(),
                    "reason", reason != null ? reason : "No reason provided",
                    "orderValue", calculateTotalOrderValue(purchaseOrder),
                    "cancelledByUserId", purchaseOrder.getUpdatedByUserId() != null ?
                            purchaseOrder.getUpdatedByUserId().toString() : "System",
                    "originalOrderDate", purchaseOrder.getOrderDate() != null ? purchaseOrder.getOrderDate().toString() : ""
            );

            // Notify the creator
            NotificationRequest.Recipient creatorRecipient = new NotificationRequest.Recipient(
                    purchaseOrder.getCreatedByUserId().toString(),
                    null, // Will be fetched by notification service
                    null,
                    null
            );

            NotificationRequest creatorNotification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "PURCHASE_ORDER_CANCELLED_CUSTOMER_INTERNAL",
                    creatorRecipient,
                    customerData,
                    null
            );

            try {
                log.info("Dispatching notification template={} channel=INTERNAL toUserId={} po={}",
                        "PURCHASE_ORDER_CANCELLED_CUSTOMER_INTERNAL",
                        purchaseOrder.getCreatedByUserId(),
                        purchaseOrder.getPurchaseOrderNumber());
                rabbitTemplate.convertAndSend("notifications.direct", "notifications.send",
                        creatorNotification);
                log.info("Notification dispatched template={} channel=INTERNAL po={} toUserId={}",
                        "PURCHASE_ORDER_CANCELLED_CUSTOMER_INTERNAL",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId());
            } catch (Exception ex) {
                log.error("Failed to dispatch template={} channel=INTERNAL po={} toUserId={}. Error: {}",
                        "PURCHASE_ORDER_CANCELLED_CUSTOMER_INTERNAL",
                        purchaseOrder.getPurchaseOrderNumber(),
                        purchaseOrder.getCreatedByUserId(),
                        ex.getMessage());
            }

            log.info("Sent purchase order cancelled notification to customer for PO: {}",
                    purchaseOrder.getPurchaseOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send purchase order cancelled notification to customer for PO: {}. Error: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Validate all line items before processing any.
     * This prevents partial failures where some items are processed but others fail.
     */
    private List<String> validateAllLineItems(PurchaseOrder purchaseOrder,
                                              List<ReceiveGoodsRequest.ReceivedLineItem> receivedItems) {
        List<String> errors = new ArrayList<>();
        Map<Long, BigDecimal> alreadyInThisRequestByLine = new HashMap<>();

        for (ReceiveGoodsRequest.ReceivedLineItem receivedItem : receivedItems) {
            Optional<PurchaseOrderLine> lineOpt = purchaseOrder.getPurchaseOrderLines().stream()
                    .filter(line -> line.getId().equals(receivedItem.getPurchaseOrderLineId()))
                    .findFirst();

            if (lineOpt.isEmpty()) {
                errors.add("Purchase order line ID " + receivedItem.getPurchaseOrderLineId() + " not found");
                continue;
            }

            PurchaseOrderLine orderLine = lineOpt.get();
            BigDecimal receivedToDate = orderLine.getReceivedQuantity() == null ? BigDecimal.ZERO
                    : orderLine.getReceivedQuantity();
            BigDecimal orderedQty = orderLine.getQuantity() == null ? BigDecimal.ZERO
                    : orderLine.getQuantity();
            BigDecimal alreadyInThisRequest = alreadyInThisRequestByLine
                    .getOrDefault(receivedItem.getPurchaseOrderLineId(), BigDecimal.ZERO);
            BigDecimal remaining = orderedQty.subtract(receivedToDate.add(alreadyInThisRequest));

            if (receivedItem.getQuantityReceived().compareTo(remaining) > 0) {
                errors.add("Line " + receivedItem.getPurchaseOrderLineId() +
                        ": Cannot receive " + receivedItem.getQuantityReceived() +
                        ". Only " + remaining.max(BigDecimal.ZERO) + " remaining " +
                        "(Ordered: " + orderedQty + ", Received: " + receivedToDate + ")");
            }

            alreadyInThisRequestByLine.put(receivedItem.getPurchaseOrderLineId(),
                    alreadyInThisRequest.add(receivedItem.getQuantityReceived()));
        }

        return errors;
    }

    /**
     * Helper method to calculate received value from GRV
     */
    private BigDecimal calculateReceivedValue(PurchaseOrder purchaseOrder, GoodsReceivedVoucher grv) {
        // This would need to be implemented based on your business logic
        // For now, returning total order value as placeholder
        return calculateTotalOrderValue(purchaseOrder);
    }

    /**
     * Publishes event when PO is approved
     * This triggers automatic Sales Order creation on supplier side
     */
    private void publishPurchaseOrderApprovedEvent(PurchaseOrder purchaseOrder,
                                                   EditPurchaseOrderRequest request,
                                                   Locale locale) {
        try {
            // Extract supplier organization ID from PO
            Long supplierOrgId = purchaseOrder.getOrganizationId(); // Supplier's org

            // Create and publish event
            PurchaseOrderApprovedEvent event = new PurchaseOrderApprovedEvent(
                    this,
                    purchaseOrder.getId(),
                    purchaseOrder.getPurchaseOrderNumber(),
                    supplierOrgId,
                    request.getApprovedByUserId() != null ?
                            request.getApprovedByUserId() : request.getUpdatedByUserId(),
                    locale
            );

            eventPublisher.publishEvent(event);

            log.info("Published PO approved event for PO {} - Sales Order will be created automatically",
                    purchaseOrder.getPurchaseOrderNumber());

        } catch (Exception e) {
            // Don't fail PO approval if event publishing fails
            log.error("Failed to publish PO approved event for PO {}: {}",
                    purchaseOrder.getId(), e.getMessage(), e);
        }
    }

    /**
     * IFRS 21: lock spot rate at order date; persist transaction and functional amounts for audit.
     */
    private void applyFunctionalCurrencyConversion(PurchaseOrder purchaseOrder) {
        Long organizationId = purchaseOrder.getOrganizationId();
        if (organizationId == null) {
            return;
        }

        if (!StringUtils.hasText(purchaseOrder.getCurrency())) {
            purchaseOrder.setCurrency(organizationFunctionalCurrencySupport.resolveFunctionalCurrency(organizationId));
        }

        String transactionCurrency = purchaseOrder.getCurrency().trim().toUpperCase();
        String functionalCurrency = organizationFunctionalCurrencySupport.resolveFunctionalCurrency(organizationId);
        purchaseOrder.setFunctionalCurrencyCode(functionalCurrency);
        LocalDate transactionDate = purchaseOrder.getOrderDate() != null ? purchaseOrder.getOrderDate() : LocalDate.now();

        if (transactionCurrency.equalsIgnoreCase(functionalCurrency)) {
            purchaseOrder.setExchangeRateUsed(BigDecimal.ONE);
            purchaseOrder.setExchangeRateSnapshotId(null);
            purchaseOrder.setSubtotalFunctional(purchaseOrder.getSubtotal());
            purchaseOrder.setTaxAmountFunctional(purchaseOrder.getTaxAmount());
            purchaseOrder.setTotalAmountFunctional(purchaseOrder.getTotalAmount());
            applyLineFunctionalAmounts(purchaseOrder, BigDecimal.ONE, null);
            return;
        }

        BillingConversionResultDto headerConversion = transactionCurrencyConversionSupport.convertToFunctionalCurrency(
                organizationId,
                transactionCurrency,
                purchaseOrder.getTotalAmount(),
                transactionDate);

        BigDecimal rate = headerConversion.getExchangeRateUsed() == null
                ? BigDecimal.ONE : headerConversion.getExchangeRateUsed();
        Long snapshotId = headerConversion.getExchangeRateSnapshotId();

        purchaseOrder.setExchangeRateUsed(rate);
        purchaseOrder.setExchangeRateSnapshotId(snapshotId);
        purchaseOrder.setSubtotalFunctional(
                transactionCurrencyConversionSupport.scaleByRate(purchaseOrder.getSubtotal(), rate));
        purchaseOrder.setTaxAmountFunctional(
                transactionCurrencyConversionSupport.scaleByRate(purchaseOrder.getTaxAmount(), rate));
        purchaseOrder.setTotalAmountFunctional(headerConversion.getConvertedAmount());
        applyLineFunctionalAmounts(purchaseOrder, rate, snapshotId);
    }

    private void applyLineFunctionalAmounts(PurchaseOrder purchaseOrder, BigDecimal rate, Long snapshotId) {
        if (purchaseOrder.getPurchaseOrderLines() == null) {
            return;
        }
        for (PurchaseOrderLine line : purchaseOrder.getPurchaseOrderLines()) {
            line.setUnitPriceFunctional(transactionCurrencyConversionSupport.scaleByRate(line.getUnitPrice(), rate));
            line.setTotalPriceFunctional(transactionCurrencyConversionSupport.scaleByRate(line.getTotalPrice(), rate));
            line.setExchangeRateSnapshotId(snapshotId);
        }
    }
}
