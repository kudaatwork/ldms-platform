package projectlx.billing.payments.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.billing.payments.business.validator.api.InvoiceServiceValidator;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class InvoiceServiceValidatorImpl implements InvoiceServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id < 1) {
            logger.info("Validation failed: invoice ID is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
