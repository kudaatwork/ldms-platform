package projectlx.billing.payments.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.billing.payments.business.auditable.api.InvoiceServiceAuditable;
import projectlx.billing.payments.business.auditable.api.PaymentServiceAuditable;
import projectlx.billing.payments.business.logic.api.PaymentService;
import projectlx.billing.payments.business.logic.support.BillingMapper;
import projectlx.billing.payments.business.logic.support.CallerOrganizationResolver;
import projectlx.billing.payments.business.logic.support.CurrencyConversionSupport;
import projectlx.billing.payments.business.validator.api.PaymentServiceValidator;
import projectlx.billing.payments.model.Invoice;
import projectlx.billing.payments.model.Payment;
import projectlx.billing.payments.repository.InvoiceRepository;
import projectlx.billing.payments.repository.PaymentRepository;
import projectlx.billing.payments.utils.config.RabbitMQProducerConfig;
import projectlx.billing.payments.utils.dtos.ConversionResultDto;
import projectlx.billing.payments.utils.enums.GatewayProvider;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.enums.InvoiceSourceType;
import projectlx.billing.payments.utils.enums.InvoiceStatus;
import projectlx.billing.payments.utils.enums.PaymentProofSource;
import projectlx.billing.payments.utils.enums.PaymentRecordStatus;
import projectlx.billing.payments.utils.requests.CreatePaymentRequest;
import projectlx.billing.payments.utils.requests.RecordProcurementPaymentRequest;
import projectlx.billing.payments.utils.responses.PaymentResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final CurrencyConversionSupport currencyConversionSupport;
    private final MessageService messageService;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentServiceAuditable paymentServiceAuditable;
    private final InvoiceServiceAuditable invoiceServiceAuditable;
    private final PaymentServiceValidator paymentServiceValidator;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse create(CreatePaymentRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request fields
        // ============================================================
        ValidatorDto validation = paymentServiceValidator.isCreatePaymentRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_PAYMENT_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Reject unsupported gateway providers
        // ============================================================
        if (request.getGatewayProvider() == GatewayProvider.PAYPAL
                || request.getGatewayProvider() == GatewayProvider.MASTERCARD) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_PAYMENT_GATEWAY_UNSUPPORTED.getCode(), new String[]{}, locale),
                    List.of());
        }

        // ============================================================
        // STEP 3: Resolve caller organisation
        // ============================================================
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale),
                    List.of());
        }

        // ============================================================
        // STEP 4: Load invoice
        // ============================================================
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndOrganizationIdAndEntityStatusNot(
                request.getInvoiceId(), organizationId, EntityStatus.DELETED);
        if (invoiceOpt.isEmpty()) {
            return error(404,
                    messageService.getMessage(I18Code.MESSAGE_INVOICE_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of());
        }

        Invoice invoice = invoiceOpt.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDate settlementDate = request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate();

        // ============================================================
        // STEP 5: Perform FX conversion
        // ============================================================
        ConversionResultDto settlementConversion = currencyConversionSupport.convertAndLockOnDate(
                invoice.getTransactionCurrencyCode(),
                invoice.getBaseCurrencyCode(),
                request.getAmountTransaction(),
                settlementDate,
                username);

        BigDecimal functionalAtOrigination = computeFunctionalAtOrigination(invoice, request.getAmountTransaction());
        BigDecimal functionalAtSettlement = settlementConversion.getConvertedAmount();
        BigDecimal realizedFxGainLoss = functionalAtSettlement.subtract(functionalAtOrigination);

        // ============================================================
        // STEP 6: Build payment entity
        // Proof upload or external reference → PENDING (awaiting verify)
        // Plain manual payment → COMPLETED immediately
        // ============================================================
        boolean requiresVerification = request.getProofDocumentId() != null
                || (request.getPaymentReferenceNumber() != null && !request.getPaymentReferenceNumber().isBlank());

        Payment payment = new Payment();
        payment.setPaymentReference("PAY-" + request.getInvoiceId() + "-" + System.currentTimeMillis());
        payment.setInvoiceId(invoice.getId());
        payment.setOrganizationId(organizationId);
        payment.setTransactionCurrencyCode(invoice.getTransactionCurrencyCode());
        payment.setBaseCurrencyCode(invoice.getBaseCurrencyCode());
        payment.setExchangeRateSnapshotId(settlementConversion.getExchangeRateSnapshotId());
        payment.setInvoiceExchangeRateSnapshotId(invoice.getExchangeRateSnapshotId());
        payment.setAmountTransaction(request.getAmountTransaction());
        payment.setAmountBase(functionalAtSettlement);
        payment.setAmountFunctionalAtOrigination(functionalAtOrigination);
        payment.setRealizedFxGainLoss(realizedFxGainLoss);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentDate(settlementDate);
        payment.setStatus(requiresVerification ? PaymentRecordStatus.PENDING : PaymentRecordStatus.COMPLETED);
        payment.setNotes(request.getNotes());
        payment.setPaymentReferenceNumber(request.getPaymentReferenceNumber());
        payment.setProofDocumentId(request.getProofDocumentId());
        payment.setProofSource(resolveProofSource(request.getProofDocumentId(), request.getProofSource()));
        payment.setGatewayProvider(request.getGatewayProvider());
        payment.setEntityStatus(EntityStatus.ACTIVE);
        payment.setCreatedAt(now);
        payment.setCreatedBy(username);

        // ============================================================
        // STEP 7: Persist via auditable and update invoice only when immediately completed
        // ============================================================
        Payment saved = paymentServiceAuditable.create(payment, locale, username);
        if (!requiresVerification) {
            updateInvoicePaymentStatus(invoice, locale);
        }

        log.info("Payment {} created with status {} for invoice {}", saved.getPaymentReference(), saved.getStatus(), invoice.getId());

        PaymentResponse response = success(201,
                messageService.getMessage(I18Code.MESSAGE_PAYMENT_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setPaymentDto(BillingMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse recordProcurementPayment(RecordProcurementPaymentRequest request,
                                                    Locale locale,
                                                    String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = paymentServiceValidator.isRecordProcurementPaymentRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_PAYMENT_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale),
                    List.of());
        }

        Optional<Invoice> invoiceOpt = invoiceRepository.findByPurchaseOrderIdAndSourceTypeAndEntityStatusNot(
                request.getPurchaseOrderId(), InvoiceSourceType.PURCHASE_ORDER, EntityStatus.DELETED);
        if (invoiceOpt.isEmpty()) {
            return error(404,
                    messageService.getMessage(I18Code.MESSAGE_INVOICE_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of("No invoice found for this purchase order."));
        }

        Invoice invoice = invoiceOpt.get();
        if (!organizationId.equals(invoice.getOrganizationId())) {
            return error(403,
                    messageService.getMessage(I18Code.MESSAGE_INVOICE_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of());
        }

        CreatePaymentRequest mapped = new CreatePaymentRequest();
        mapped.setInvoiceId(invoice.getId());
        mapped.setAmountTransaction(request.getAmount());
        mapped.setPaymentMethod(request.getPaymentMethod());
        mapped.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        mapped.setNotes(request.getNotes());
        mapped.setPaymentReferenceNumber(request.getReferenceNumber());
        mapped.setProofDocumentId(request.getProofDocumentId());
        mapped.setProofSource(request.getProofSource() != null
                ? request.getProofSource()
                : PaymentProofSource.SYSTEM_GENERATED);
        return create(mapped, locale, username);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse verifyPayment(Long paymentId, String username, Locale locale) {

        // ============================================================
        // STEP 1: Validate paymentId
        // ============================================================
        ValidatorDto idValidation = paymentServiceValidator.isIdValid(paymentId, locale);
        if (!idValidation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_PAYMENT_VERIFY_INVALID.getCode(), new String[]{}, locale),
                    idValidation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load payment with pessimistic lock
        // ============================================================
        Optional<Payment> paymentOpt = paymentRepository.findByIdAndEntityStatusNot(paymentId, EntityStatus.DELETED);
        if (paymentOpt.isEmpty()) {
            return error(404,
                    messageService.getMessage(I18Code.MESSAGE_PAYMENT_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of());
        }

        Payment payment = paymentOpt.get();

        // ============================================================
        // STEP 3: Guard - only PENDING payments can be verified
        // ============================================================
        if (payment.getStatus() != PaymentRecordStatus.PENDING) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_PAYMENT_VERIFY_INVALID.getCode(), new String[]{}, locale),
                    List.of("Payment is not in PENDING status; current status: " + payment.getStatus()));
        }

        // ============================================================
        // STEP 4: Mark VERIFIED then COMPLETED, stamp audits
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentRecordStatus.VERIFIED);
        payment.setVerifiedAt(now);
        payment.setVerifiedBy(username);
        payment.setModifiedAt(now);
        payment.setModifiedBy(username);
        paymentServiceAuditable.update(payment, locale, username);

        // ============================================================
        // STEP 5: Update invoice payment status
        // ============================================================
        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndEntityStatusNot(payment.getInvoiceId(), EntityStatus.DELETED);
        if (invoiceOpt.isPresent()) {
            updateInvoicePaymentStatus(invoiceOpt.get(), locale);
        } else {
            log.warn("Invoice {} not found while verifying payment {}", payment.getInvoiceId(), paymentId);
        }

        // ============================================================
        // STEP 6: Publish payment.verified event with purchaseOrderId from invoice
        // ============================================================
        try {
            Long purchaseOrderId = invoiceOpt.map(Invoice::getPurchaseOrderId).orElse(null);
            Map<String, Object> event = buildPaymentVerifiedEvent(payment, purchaseOrderId);
            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.BILLING_EXCHANGE,
                    RabbitMQProducerConfig.PAYMENT_VERIFIED_ROUTING_KEY,
                    event);
            log.info("Published payment.verified event for payment {} with purchaseOrderId {}", paymentId, purchaseOrderId);
        } catch (Exception ex) {
            log.error("Failed to publish payment.verified event for payment {}: {}", paymentId, ex.getMessage(), ex);
        }

        PaymentResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_PAYMENT_VERIFY_SUCCESS.getCode(), new String[]{}, locale));
        response.setPaymentDto(BillingMapper.toDto(payment));
        return response;
    }

    private Map<String, Object> buildPaymentVerifiedEvent(Payment payment, Long purchaseOrderId) {
        Map<String, Object> event = new HashMap<>();
        event.put("paymentId", payment.getId());
        event.put("paymentReference", payment.getPaymentReference());
        event.put("invoiceId", payment.getInvoiceId());
        event.put("organizationId", payment.getOrganizationId());
        event.put("purchaseOrderId", purchaseOrderId);
        event.put("amountTransaction", payment.getAmountTransaction());
        event.put("transactionCurrencyCode", payment.getTransactionCurrencyCode());
        event.put("verifiedBy", payment.getVerifiedBy());
        event.put("verifiedAt", payment.getVerifiedAt() != null ? payment.getVerifiedAt().toString() : null);
        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse listByInvoice(Long invoiceId, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale),
                    List.of());
        }

        Optional<Invoice> invoiceOpt = invoiceRepository.findByIdAndOrganizationIdAndEntityStatusNot(
                invoiceId, organizationId, EntityStatus.DELETED);
        if (invoiceOpt.isEmpty()) {
            return error(404,
                    messageService.getMessage(I18Code.MESSAGE_INVOICE_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of());
        }

        PaymentResponse response = success(200, "Payments retrieved");
        response.setPaymentDtoList(paymentRepository
                .findByInvoiceIdAndEntityStatusNotOrderByPaymentDateDesc(invoiceId, EntityStatus.DELETED)
                .stream()
                .map(BillingMapper::toDto)
                .toList());
        return response;
    }

    private void updateInvoicePaymentStatus(Invoice invoice, Locale locale) {
        BigDecimal paid = paymentRepository.findByInvoiceIdAndEntityStatusNotOrderByPaymentDateDesc(
                        invoice.getId(), EntityStatus.DELETED)
                .stream()
                .filter(p -> p.getStatus() == PaymentRecordStatus.VERIFIED
                        || p.getStatus() == PaymentRecordStatus.COMPLETED)
                .map(Payment::getAmountTransaction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (paid.compareTo(invoice.getTotalTransaction()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoice.setModifiedAt(LocalDateTime.now());
        invoice.setModifiedBy("SYSTEM");
        invoiceServiceAuditable.update(invoice, locale, "SYSTEM");
    }

    /**
     * IFRS 21: functional amount at the invoice transaction-date spot rate (origination).
     */
    private BigDecimal computeFunctionalAtOrigination(Invoice invoice, BigDecimal amountTransaction) {
        if (amountTransaction == null) {
            return BigDecimal.ZERO;
        }
        if (invoice.getTransactionCurrencyCode().equalsIgnoreCase(invoice.getBaseCurrencyCode())) {
            return amountTransaction.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal invoiceTotalTransaction = invoice.getTotalTransaction();
        if (invoiceTotalTransaction == null || invoiceTotalTransaction.compareTo(BigDecimal.ZERO) == 0) {
            return amountTransaction.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal originationRate = invoice.getTotalBase()
                .divide(invoiceTotalTransaction, 8, RoundingMode.HALF_UP);
        return amountTransaction.multiply(originationRate).setScale(4, RoundingMode.HALF_UP);
    }

    private PaymentProofSource resolveProofSource(Long proofDocumentId, PaymentProofSource explicit) {
        if (explicit != null) {
            return explicit;
        }
        return proofDocumentId != null ? PaymentProofSource.EXTERNAL_UPLOAD : PaymentProofSource.SYSTEM_GENERATED;
    }

    private PaymentResponse success(int statusCode, String message) {
        PaymentResponse response = new PaymentResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private PaymentResponse error(int statusCode, String message, List<String> errors) {
        PaymentResponse response = new PaymentResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
