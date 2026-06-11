package projectlx.billing.payments.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface InvoiceServiceValidator {
    ValidatorDto isIdValid(Long id, Locale locale);
}
