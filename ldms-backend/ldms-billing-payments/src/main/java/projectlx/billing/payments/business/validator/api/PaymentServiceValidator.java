package projectlx.billing.payments.business.validator.api;

import projectlx.billing.payments.utils.requests.CreatePaymentRequest;
import projectlx.billing.payments.utils.requests.RecordProcurementPaymentRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface PaymentServiceValidator {
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isCreatePaymentRequestValid(CreatePaymentRequest request, Locale locale);
    ValidatorDto isRecordProcurementPaymentRequestValid(RecordProcurementPaymentRequest request, Locale locale);
}
