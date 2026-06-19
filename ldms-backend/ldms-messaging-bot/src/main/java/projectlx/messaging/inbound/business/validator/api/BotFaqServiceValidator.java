package projectlx.messaging.inbound.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.messaging.inbound.utils.requests.CreateBotFaqRequest;
import projectlx.messaging.inbound.utils.requests.EditBotFaqRequest;

import java.util.Locale;

public interface BotFaqServiceValidator {

    ValidatorDto isCreateRequestValid(CreateBotFaqRequest request, Locale locale);

    ValidatorDto isEditRequestValid(EditBotFaqRequest request, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);
}
