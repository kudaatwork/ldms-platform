package projectlx.co.zw.organizationmanagement.business.validation.api;

import projectlx.co.zw.organizationmanagement.utils.requests.CreateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateTradingPartnerRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface TradingPartnerServiceValidator {

    ValidatorDto validateCreate(CreateTradingPartnerRequest request, Locale locale);

    ValidatorDto validateUpdate(UpdateTradingPartnerRequest request, Locale locale);

    ValidatorDto validateId(Long id, Locale locale);
}
