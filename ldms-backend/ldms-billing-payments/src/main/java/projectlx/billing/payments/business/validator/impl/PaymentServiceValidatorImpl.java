package projectlx.billing.payments.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.billing.payments.business.validator.api.PaymentServiceValidator;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.enums.PaymentProofSource;
import projectlx.billing.payments.utils.requests.CreatePaymentRequest;
import projectlx.billing.payments.utils.requests.RecordProcurementPaymentRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class PaymentServiceValidatorImpl implements PaymentServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id < 1) {
            logger.info("Validation failed: payment ID is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isCreatePaymentRequestValid(CreatePaymentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreatePaymentRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getInvoiceId() == null || request.getInvoiceId() < 1) {
            logger.info("Validation failed: invoiceId is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"invoiceId"}, locale));
        }

        if (request.getAmountTransaction() == null || request.getAmountTransaction().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Validation failed: amountTransaction is null or not positive");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"amountTransaction"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isRecordProcurementPaymentRequestValid(RecordProcurementPaymentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: RecordProcurementPaymentRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseOrderId() == null || request.getPurchaseOrderId() < 1) {
            logger.info("Validation failed: purchaseOrderId is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"purchaseOrderId"}, locale));
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Validation failed: amount is null or not positive");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"amount"}, locale));
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            logger.info("Validation failed: paymentMethod is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"paymentMethod"}, locale));
        }

        if (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank()) {
            logger.info("Validation failed: referenceNumber is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"referenceNumber"}, locale));
        }

        PaymentProofSource source = request.getProofSource() != null
                ? request.getProofSource()
                : PaymentProofSource.SYSTEM_GENERATED;
        if (source == PaymentProofSource.EXTERNAL_UPLOAD) {
            if (request.getProofDocumentId() == null || request.getProofDocumentId() < 1) {
                logger.info("Validation failed: proofDocumentId required for EXTERNAL_UPLOAD");
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"proofDocumentId"}, locale));
            }
        } else if (request.getProofDocumentId() != null) {
            logger.info("Validation failed: proofDocumentId must not be set for system-recorded payments");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_PAYMENT_CREATE_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }
}
