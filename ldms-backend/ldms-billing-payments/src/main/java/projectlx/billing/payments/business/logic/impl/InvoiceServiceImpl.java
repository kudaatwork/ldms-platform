package projectlx.billing.payments.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import projectlx.billing.payments.business.auditable.api.InvoiceLineServiceAuditable;
import projectlx.billing.payments.business.auditable.api.InvoiceServiceAuditable;
import projectlx.billing.payments.business.logic.api.InvoiceService;
import projectlx.billing.payments.business.logic.support.BillingMapper;
import projectlx.billing.payments.business.logic.support.CallerOrganizationResolver;
import projectlx.billing.payments.business.logic.support.CurrencyConversionSupport;
import projectlx.billing.payments.business.validator.api.InvoiceServiceValidator;
import projectlx.billing.payments.clients.InventoryManagementServiceClient;
import projectlx.billing.payments.model.Invoice;
import projectlx.billing.payments.model.InvoiceLine;
import projectlx.billing.payments.repository.InvoiceLineRepository;
import projectlx.billing.payments.repository.InvoiceRepository;
import projectlx.billing.payments.utils.dtos.ConversionResultDto;
import projectlx.billing.payments.utils.dtos.InventoryPurchaseOrderDto;
import projectlx.billing.payments.utils.dtos.InventoryPurchaseOrderLineDto;
import projectlx.billing.payments.utils.dtos.InvoiceDto;
import projectlx.billing.payments.utils.responses.InventoryPurchaseOrderResponse;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.enums.InvoiceSourceType;
import projectlx.billing.payments.utils.enums.InvoiceStatus;
import projectlx.billing.payments.utils.requests.InventoryPurchaseOrderLineFilterRequest;
import projectlx.billing.payments.utils.responses.InventoryPurchaseOrderLineResponse;
import projectlx.billing.payments.utils.responses.InvoiceResponse;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final CurrencyConversionSupport currencyConversionSupport;
    private final InventoryManagementServiceClient inventoryManagementServiceClient;
    private final MessageService messageService;
    private final InvoiceServiceAuditable invoiceServiceAuditable;
    private final InvoiceLineServiceAuditable invoiceLineServiceAuditable;
    private final InvoiceServiceValidator invoiceServiceValidator;

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        List<InvoiceDto> invoices = invoiceRepository
                .findByOrganizationIdAndEntityStatusNotOrderByIssuedAtDesc(organizationId, EntityStatus.DELETED)
                .stream()
                .map(invoice -> {
                    InvoiceDto dto = BillingMapper.toDto(invoice);
                    dto.setLines(invoiceLineRepository
                            .findByInvoiceIdAndEntityStatusNotOrderByLineNumberAsc(invoice.getId(), EntityStatus.DELETED)
                            .stream()
                            .map(BillingMapper::toDto)
                            .toList());
                    return dto;
                })
                .toList();

        InvoiceResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_INVOICE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setInvoiceDtoList(invoices);
        return response;
    }

    @Override
    public InvoiceResponse generateFromGrvEvent(Map<String, Object> event, Locale locale) {
        Long grvId = parseLong(event.get("grvId"));
        if (grvId == null) {
            log.debug("GRV invoice generation skipped: missing grvId");
            return success(200, "Skipped");
        }

        if (invoiceRepository.findByGrvIdAndEntityStatusNot(grvId, EntityStatus.DELETED).isPresent()) {
            log.info("Invoice already exists for GRV {}", grvId);
            return success(200, "Already invoiced");
        }

        Long organizationId = parseLong(event.get("organizationId"));
        Long purchaseOrderId = parseLong(event.get("purchaseOrderId"));
        String grvNumber = stringValue(event.get("grvNumber"));
        String purchaseOrderNumber = stringValue(event.get("purchaseOrderNumber"));
        Long supplierId = parseLong(event.get("supplierId"));

        InventoryPurchaseOrderDto purchaseOrder = fetchPurchaseOrder(purchaseOrderId, locale);

        String transactionCurrency = purchaseOrder != null && purchaseOrder.getCurrency() != null
                ? purchaseOrder.getCurrency() : "USD";
        String baseCurrency = purchaseOrder != null && purchaseOrder.getFunctionalCurrencyCode() != null
                ? purchaseOrder.getFunctionalCurrencyCode()
                : currencyConversionSupport.resolveFunctionalCurrencyForOrganization(organizationId, null);
        PaymentTerm paymentTerm = purchaseOrder != null && purchaseOrder.getPaymentTerm() != null
                ? purchaseOrder.getPaymentTerm() : PaymentTerm.NET_30;
        LocalDate paymentDueDate = purchaseOrder != null && purchaseOrder.getPaymentDueDate() != null
                ? purchaseOrder.getPaymentDueDate() : LocalDate.now().plusDays(30);

        List<InventoryPurchaseOrderLineDto> poLines = fetchPurchaseOrderLines(purchaseOrderId, locale);
        BigDecimal subtotalTransaction = purchaseOrder != null && purchaseOrder.getSubtotal() != null
                ? purchaseOrder.getSubtotal()
                : poLines.stream()
                        .map(line -> line.getTotalPrice() == null ? BigDecimal.ZERO : line.getTotalPrice())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxTransaction = purchaseOrder != null && purchaseOrder.getTaxAmount() != null
                ? purchaseOrder.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalTransaction = purchaseOrder != null && purchaseOrder.getTotalAmount() != null
                ? purchaseOrder.getTotalAmount()
                : subtotalTransaction.add(taxTransaction);

        LocalDate transactionDate = purchaseOrder != null && purchaseOrder.getOrderDate() != null
                ? purchaseOrder.getOrderDate() : LocalDate.now();
        LocalDateTime issuedAt = transactionDate.atStartOfDay();

        ConversionResultDto conversion;
        try {
            if (purchaseOrder != null && purchaseOrder.getTotalAmountFunctional() != null
                    && purchaseOrder.getExchangeRateSnapshotId() != null) {
                conversion = new ConversionResultDto();
                conversion.setFromCurrencyCode(transactionCurrency);
                conversion.setToCurrencyCode(baseCurrency);
                conversion.setSourceAmount(totalTransaction);
                conversion.setConvertedAmount(purchaseOrder.getTotalAmountFunctional());
                conversion.setExchangeRateUsed(purchaseOrder.getExchangeRateUsed());
                conversion.setExchangeRateSnapshotId(purchaseOrder.getExchangeRateSnapshotId());
            } else {
                conversion = currencyConversionSupport.convertAndLockOnDate(
                        transactionCurrency, baseCurrency, totalTransaction, transactionDate, "SYSTEM");
            }
        } catch (IllegalStateException ex) {
            log.warn("No exchange rate for {} -> {}; using 1:1 for GRV {}", transactionCurrency, baseCurrency, grvId);
            conversion = new ConversionResultDto();
            conversion.setFromCurrencyCode(transactionCurrency);
            conversion.setToCurrencyCode(baseCurrency);
            conversion.setSourceAmount(subtotalTransaction);
            conversion.setConvertedAmount(subtotalTransaction);
            conversion.setExchangeRateUsed(BigDecimal.ONE);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber(grvId));
        invoice.setOrganizationId(organizationId == null ? 0L : organizationId);
        invoice.setSupplierId(supplierId);
        invoice.setSourceType(InvoiceSourceType.GRV);
        invoice.setSourceId(grvId);
        invoice.setSourceReference(grvNumber);
        invoice.setGrvId(grvId);
        invoice.setGrvNumber(grvNumber);
        invoice.setPurchaseOrderId(purchaseOrderId);
        invoice.setPurchaseOrderNumber(purchaseOrderNumber);
        invoice.setTransactionCurrencyCode(transactionCurrency);
        invoice.setBaseCurrencyCode(baseCurrency);
        invoice.setExchangeRateSnapshotId(conversion.getExchangeRateSnapshotId());
        BigDecimal subtotalBase = purchaseOrder != null && purchaseOrder.getSubtotalFunctional() != null
                ? purchaseOrder.getSubtotalFunctional()
                : scaleFunctional(subtotalTransaction, conversion);
        BigDecimal taxBase = purchaseOrder != null && purchaseOrder.getTaxAmountFunctional() != null
                ? purchaseOrder.getTaxAmountFunctional()
                : scaleFunctional(taxTransaction, conversion);
        BigDecimal totalBase = conversion.getConvertedAmount();

        invoice.setSubtotalTransaction(subtotalTransaction);
        invoice.setSubtotalBase(subtotalBase);
        invoice.setTaxTransaction(taxTransaction);
        invoice.setTaxBase(taxBase);
        invoice.setTotalTransaction(totalTransaction);
        invoice.setTotalBase(totalBase);
        invoice.setPaymentTerm(paymentTerm);
        invoice.setPaymentDueDate(paymentDueDate);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(issuedAt);
        invoice.setEntityStatus(EntityStatus.ACTIVE);
        invoice.setCreatedAt(issuedAt);
        invoice.setCreatedBy("SYSTEM");

        Invoice saved = invoiceServiceAuditable.create(invoice, locale, "SYSTEM");
        saveInvoiceLines(saved, poLines, conversion, issuedAt, "SYSTEM");

        InvoiceDto dto = BillingMapper.toDto(saved);
        dto.setLines(invoiceLineRepository
                .findByInvoiceIdAndEntityStatusNotOrderByLineNumberAsc(saved.getId(), EntityStatus.DELETED)
                .stream()
                .map(BillingMapper::toDto)
                .toList());

        InvoiceResponse response = success(201,
                messageService.getMessage(I18Code.MESSAGE_INVOICE_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setInvoiceDto(dto);
        log.info("Generated invoice {} for GRV {}", saved.getInvoiceNumber(), grvNumber);
        return response;
    }

    @Override
    public InvoiceResponse generateFromPurchaseOrderEvent(Map<String, Object> event, Locale locale) {

        // ============================================================
        // STEP 1: Extract purchaseOrderId from event payload
        // ============================================================
        Long purchaseOrderId = parseLong(event.get("purchaseOrderId"));
        if (purchaseOrderId == null) {
            log.debug("PO invoice generation skipped: missing purchaseOrderId");
            return success(200, "Skipped");
        }

        // ============================================================
        // STEP 2: Idempotency check – skip if invoice already exists for this PO
        // ============================================================
        if (invoiceRepository.findByPurchaseOrderIdAndSourceTypeAndEntityStatusNot(
                purchaseOrderId, InvoiceSourceType.PURCHASE_ORDER, EntityStatus.DELETED).isPresent()) {
            log.info("Invoice already exists for PO {}", purchaseOrderId);
            return success(200, "Already invoiced");
        }

        // ============================================================
        // STEP 3: Extract event metadata
        // ============================================================
        Long organizationId = parseLong(event.get("organizationId"));
        String purchaseOrderNumber = stringValue(event.get("purchaseOrderNumber"));
        Long supplierId = parseLong(event.get("supplierId"));

        // ============================================================
        // STEP 4: Fetch PO and lines from inventory service
        // ============================================================
        InventoryPurchaseOrderDto purchaseOrder = fetchPurchaseOrder(purchaseOrderId, locale);

        String transactionCurrency = purchaseOrder != null && purchaseOrder.getCurrency() != null
                ? purchaseOrder.getCurrency() : "USD";
        String baseCurrency = purchaseOrder != null && purchaseOrder.getFunctionalCurrencyCode() != null
                ? purchaseOrder.getFunctionalCurrencyCode()
                : currencyConversionSupport.resolveFunctionalCurrencyForOrganization(organizationId, null);
        PaymentTerm paymentTerm = purchaseOrder != null && purchaseOrder.getPaymentTerm() != null
                ? purchaseOrder.getPaymentTerm() : PaymentTerm.NET_30;
        LocalDate paymentDueDate = purchaseOrder != null && purchaseOrder.getPaymentDueDate() != null
                ? purchaseOrder.getPaymentDueDate() : LocalDate.now().plusDays(30);

        List<InventoryPurchaseOrderLineDto> poLines = fetchPurchaseOrderLines(purchaseOrderId, locale);
        BigDecimal subtotalTransaction = purchaseOrder != null && purchaseOrder.getSubtotal() != null
                ? purchaseOrder.getSubtotal()
                : poLines.stream()
                        .map(line -> line.getTotalPrice() == null ? BigDecimal.ZERO : line.getTotalPrice())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxTransaction = purchaseOrder != null && purchaseOrder.getTaxAmount() != null
                ? purchaseOrder.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalTransaction = purchaseOrder != null && purchaseOrder.getTotalAmount() != null
                ? purchaseOrder.getTotalAmount()
                : subtotalTransaction.add(taxTransaction);

        LocalDate transactionDate = purchaseOrder != null && purchaseOrder.getOrderDate() != null
                ? purchaseOrder.getOrderDate() : LocalDate.now();
        LocalDateTime issuedAt = transactionDate.atStartOfDay();

        // ============================================================
        // STEP 5: Resolve FX conversion
        // ============================================================
        ConversionResultDto conversion;
        try {
            if (purchaseOrder != null && purchaseOrder.getTotalAmountFunctional() != null
                    && purchaseOrder.getExchangeRateSnapshotId() != null) {
                conversion = new ConversionResultDto();
                conversion.setFromCurrencyCode(transactionCurrency);
                conversion.setToCurrencyCode(baseCurrency);
                conversion.setSourceAmount(totalTransaction);
                conversion.setConvertedAmount(purchaseOrder.getTotalAmountFunctional());
                conversion.setExchangeRateUsed(purchaseOrder.getExchangeRateUsed());
                conversion.setExchangeRateSnapshotId(purchaseOrder.getExchangeRateSnapshotId());
            } else {
                conversion = currencyConversionSupport.convertAndLockOnDate(
                        transactionCurrency, baseCurrency, totalTransaction, transactionDate, "SYSTEM");
            }
        } catch (IllegalStateException ex) {
            log.warn("No exchange rate for {} -> {}; using 1:1 for PO {}", transactionCurrency, baseCurrency, purchaseOrderId);
            conversion = new ConversionResultDto();
            conversion.setFromCurrencyCode(transactionCurrency);
            conversion.setToCurrencyCode(baseCurrency);
            conversion.setSourceAmount(totalTransaction);
            conversion.setConvertedAmount(totalTransaction);
            conversion.setExchangeRateUsed(BigDecimal.ONE);
        }

        // ============================================================
        // STEP 6: Build and persist invoice via auditable
        // ============================================================
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-PO-" + purchaseOrderId + "-" + System.currentTimeMillis());
        invoice.setOrganizationId(organizationId == null ? 0L : organizationId);
        invoice.setSupplierId(supplierId);
        invoice.setSourceType(InvoiceSourceType.PURCHASE_ORDER);
        invoice.setSourceId(purchaseOrderId);
        invoice.setSourceReference(purchaseOrderNumber);
        invoice.setPurchaseOrderId(purchaseOrderId);
        invoice.setPurchaseOrderNumber(purchaseOrderNumber);
        invoice.setTransactionCurrencyCode(transactionCurrency);
        invoice.setBaseCurrencyCode(baseCurrency);
        invoice.setExchangeRateSnapshotId(conversion.getExchangeRateSnapshotId());

        BigDecimal subtotalBase = purchaseOrder != null && purchaseOrder.getSubtotalFunctional() != null
                ? purchaseOrder.getSubtotalFunctional()
                : scaleFunctional(subtotalTransaction, conversion);
        BigDecimal taxBase = purchaseOrder != null && purchaseOrder.getTaxAmountFunctional() != null
                ? purchaseOrder.getTaxAmountFunctional()
                : scaleFunctional(taxTransaction, conversion);

        invoice.setSubtotalTransaction(subtotalTransaction);
        invoice.setSubtotalBase(subtotalBase);
        invoice.setTaxTransaction(taxTransaction);
        invoice.setTaxBase(taxBase);
        invoice.setTotalTransaction(totalTransaction);
        invoice.setTotalBase(conversion.getConvertedAmount());
        invoice.setPaymentTerm(paymentTerm);
        invoice.setPaymentDueDate(paymentDueDate);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(issuedAt);
        invoice.setEntityStatus(EntityStatus.ACTIVE);
        invoice.setCreatedAt(issuedAt);
        invoice.setCreatedBy("SYSTEM");

        Invoice saved = invoiceServiceAuditable.create(invoice, locale, "SYSTEM");
        saveInvoiceLines(saved, poLines, conversion, issuedAt, "SYSTEM");

        InvoiceDto dto = BillingMapper.toDto(saved);
        dto.setLines(invoiceLineRepository
                .findByInvoiceIdAndEntityStatusNotOrderByLineNumberAsc(saved.getId(), EntityStatus.DELETED)
                .stream()
                .map(BillingMapper::toDto)
                .toList());

        InvoiceResponse response = success(201,
                messageService.getMessage(I18Code.MESSAGE_INVOICE_PO_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setInvoiceDto(dto);
        log.info("Generated invoice {} for PO {}", saved.getInvoiceNumber(), purchaseOrderNumber);
        return response;
    }

    private void saveInvoiceLines(
            Invoice invoice,
            List<InventoryPurchaseOrderLineDto> poLines,
            ConversionResultDto headerConversion,
            LocalDateTime issuedAt,
            String actor) {

        List<InvoiceLine> lines = new ArrayList<>();
        int lineNumber = 1;

        if (poLines.isEmpty()) {
            InvoiceLine line = buildLine(invoice.getId(), lineNumber++, "GRV " + invoice.getGrvNumber(),
                    BigDecimal.ONE, invoice.getTotalTransaction(), headerConversion, issuedAt, actor);
            lines.add(line);
        } else {
            for (InventoryPurchaseOrderLineDto poLine : poLines) {
                BigDecimal qty = poLine.getQuantity() == null ? BigDecimal.ONE : poLine.getQuantity();
                BigDecimal unitPrice = poLine.getUnitPrice() == null ? BigDecimal.ZERO : poLine.getUnitPrice();
                BigDecimal lineTotal = poLine.getTotalPrice() == null ? qty.multiply(unitPrice) : poLine.getTotalPrice();

                BigDecimal lineTotalBase = poLine.getTotalPriceFunctional() != null
                        ? poLine.getTotalPriceFunctional()
                        : scaleFunctional(lineTotal, headerConversion);
                BigDecimal unitPriceBase = poLine.getUnitPriceFunctional() != null
                        ? poLine.getUnitPriceFunctional()
                        : unitPrice.multiply(headerConversion.getExchangeRateUsed());

                InvoiceLine line = buildLine(invoice.getId(), lineNumber++,
                        "PO line " + poLine.getId(), qty, unitPrice, headerConversion, issuedAt, actor);
                line.setLineTotalTransaction(lineTotal);
                line.setLineTotalBase(lineTotalBase);
                line.setUnitPriceBase(unitPriceBase);
                lines.add(line);
            }
        }

        invoiceLineServiceAuditable.createAll(lines, actor);
    }

    private InvoiceLine buildLine(
            Long invoiceId,
            int lineNumber,
            String description,
            BigDecimal quantity,
            BigDecimal unitPriceTransaction,
            ConversionResultDto conversion,
            LocalDateTime issuedAt,
            String actor) {

        InvoiceLine line = new InvoiceLine();
        line.setInvoiceId(invoiceId);
        line.setLineNumber(lineNumber);
        line.setDescription(description);
        line.setQuantity(quantity);
        line.setUnitPriceTransaction(unitPriceTransaction);
        line.setLineTotalTransaction(quantity.multiply(unitPriceTransaction));
        BigDecimal unitBase = unitPriceTransaction.multiply(conversion.getExchangeRateUsed());
        line.setUnitPriceBase(unitBase);
        line.setLineTotalBase(line.getLineTotalTransaction().multiply(conversion.getExchangeRateUsed()));
        line.setExchangeRateSnapshotId(conversion.getExchangeRateSnapshotId());
        line.setEntityStatus(EntityStatus.ACTIVE);
        line.setCreatedAt(issuedAt);
        line.setCreatedBy(actor);
        return line;
    }

    private InventoryPurchaseOrderDto fetchPurchaseOrder(Long purchaseOrderId, Locale locale) {
        if (purchaseOrderId == null) {
            return null;
        }
        try {
            InventoryPurchaseOrderResponse response =
                    inventoryManagementServiceClient.findPurchaseOrderById(purchaseOrderId, locale);
            if (response != null && response.isSuccess() && response.getPurchaseOrderDto() != null) {
                return response.getPurchaseOrderDto();
            }
        } catch (Exception ex) {
            log.warn("Could not fetch PO {}: {}", purchaseOrderId, ex.getMessage());
        }
        return null;
    }

    private BigDecimal scaleFunctional(BigDecimal transactionAmount, ConversionResultDto conversion) {
        if (transactionAmount == null || conversion == null || conversion.getExchangeRateUsed() == null) {
            return BigDecimal.ZERO;
        }
        return transactionAmount.multiply(conversion.getExchangeRateUsed())
                .setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private List<InventoryPurchaseOrderLineDto> fetchPurchaseOrderLines(Long purchaseOrderId, Locale locale) {
        if (purchaseOrderId == null) {
            return List.of();
        }
        try {
            InventoryPurchaseOrderLineFilterRequest filter = new InventoryPurchaseOrderLineFilterRequest();
            filter.setPurchaseOrderId(purchaseOrderId);
            filter.setPage(0);
            filter.setSize(500);
            InventoryPurchaseOrderLineResponse response =
                    inventoryManagementServiceClient.findPurchaseOrderLinesByFilters(filter, locale);
            if (response != null && response.isSuccess() && response.getPurchaseOrderLineDtoList() != null) {
                return response.getPurchaseOrderLineDtoList();
            }
        } catch (Exception ex) {
            log.warn("Could not fetch PO lines for PO {}: {}", purchaseOrderId, ex.getMessage());
        }
        return List.of();
    }

    private String generateInvoiceNumber(Long grvId) {
        return "INV-GRV-" + grvId + "-" + System.currentTimeMillis();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private InvoiceResponse success(int statusCode, String message) {
        InvoiceResponse response = new InvoiceResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private InvoiceResponse error(int statusCode, String message) {
        InvoiceResponse response = new InvoiceResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
