package projectlx.co.zw.organizationmanagement.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.organizationmanagement.business.validation.api.TradingPartnerServiceValidator;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateTradingPartnerRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class TradingPartnerServiceValidatorImpl implements TradingPartnerServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(TradingPartnerServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto validateCreate(CreateTradingPartnerRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("TradingPartner create validation failed: request is null");
            errors.add(messageService.getMessage(I18Code.TRADING_PARTNER_VALIDATION_FAILED.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPartnerRole() == null) {
            errors.add(messageService.getMessage(I18Code.TRADING_PARTNER_VALIDATION_FAILED.getCode(),
                    new String[]{"partnerRole is required"}, locale));
        }

        if (!StringUtils.hasText(request.getName())) {
            errors.add(messageService.getMessage(I18Code.TRADING_PARTNER_VALIDATION_FAILED.getCode(),
                    new String[]{"name is required"}, locale));
        }

        if (!errors.isEmpty()) {
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto validateUpdate(UpdateTradingPartnerRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("TradingPartner update validation failed: request is null");
            errors.add(messageService.getMessage(I18Code.TRADING_PARTNER_VALIDATION_FAILED.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!errors.isEmpty()) {
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto validateId(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (id == null || id <= 0) {
            errors.add(messageService.getMessage(I18Code.TRADING_PARTNER_VALIDATION_FAILED.getCode(),
                    new String[]{"id is required"}, locale));
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }
}
